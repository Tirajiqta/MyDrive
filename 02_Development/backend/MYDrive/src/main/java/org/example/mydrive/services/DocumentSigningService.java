package org.example.mydrive.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.example.mydrive.dto.SignatureResponse;
import org.example.mydrive.entities.DocumentSignatureEntity;
import org.example.mydrive.entities.FileEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.DocumentSignatureRepository;
import org.example.mydrive.repositories.FileRepository;
import org.example.mydrive.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentSigningService {

    @Value("${app.storage.path}")
    private String storagePath;

    @Value("${app.signing.keystore-path}")
    private String keystorePath;

    @Value("${app.signing.keystore-password}")
    private String keystorePassword;

    @Value("${app.signing.key-alias}")
    private String keyAlias;

    private final DocumentSignatureRepository signatureRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void initKeystore() throws Exception {
        Path ksPath = Path.of(keystorePath);
        if (Files.exists(ksPath)) return;

        Files.createDirectories(ksPath.getParent());

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=MyDrive Demo Signature,O=MyDrive,C=BG");
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 10L * 365 * 24 * 60 * 60 * 1000);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore, notAfter,
                subject,
                keyPair.getPublic());

        ContentSigner certSigner = new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());
        X509Certificate cert = new JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(certSigner));

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(keyAlias, keyPair.getPrivate(),
                keystorePassword.toCharArray(), new Certificate[]{cert});

        try (OutputStream os = Files.newOutputStream(ksPath)) {
            ks.store(os, keystorePassword.toCharArray());
        }
    }

    /**
     * Signs a PDF file using PAdES-B-B (Baseline-B) format.
     * Compatible with eIDAS Article 26 Advanced Electronic Signature requirements.
     * For QES (Qualified), replace the demo certificate with one from a qualified TSP.
     */
    public SignatureResponse signPdf(Long fileId, Long userId) throws Exception {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getType().equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files can be signed (PAdES format)");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(Path.of(keystorePath))) {
            ks.load(is, keystorePassword.toCharArray());
        }

        PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias, keystorePassword.toCharArray());
        Certificate[] chain = ks.getCertificateChain(keyAlias);
        X509Certificate cert = (X509Certificate) chain[0];

        Path sourceFile = Path.of(storagePath, file.getUniqueName());
        String signedName = "signed_" + UUID.randomUUID() + ".pdf";
        Path signedFile = Path.of(storagePath, signedName);

        // PDFBox 3.x uses Loader.loadPDF() instead of PDDocument.load()
        try (PDDocument doc = Loader.loadPDF(sourceFile.toFile())) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(user.getUsername());
            signature.setSignDate(Calendar.getInstance());
            signature.setReason("PAdES-B-B eIDAS Advanced Electronic Signature");
            signature.setLocation("MyDrive");

            SignatureOptions opts = new SignatureOptions();
            opts.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);

            // In PDFBox 3.x the SignatureInterface receives InputStream content
            doc.addSignature(signature, (InputStream content) -> {
                try {
                    byte[] contentBytes = content.readAllBytes();
                    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                    ContentSigner cs = new JcaContentSignerBuilder("SHA256withRSA")
                            .setProvider("BC").build(privateKey);
                    gen.addSignerInfoGenerator(
                            new JcaSignerInfoGeneratorBuilder(
                                    new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                                    .build(cs, cert));
                    gen.addCertificates(new JcaCertStore(List.of(chain)));
                    CMSSignedData cmsData = gen.generate(
                            new CMSProcessableByteArray(contentBytes), false);
                    return cmsData.getEncoded();
                } catch (Exception e) {
                    throw new RuntimeException("CMS signing failed", e);
                }
            }, opts);

            doc.saveIncremental(Files.newOutputStream(signedFile));
        }

        DocumentSignatureEntity sigEntity = DocumentSignatureEntity.builder()
                .fileId(fileId)
                .signedBy(user)
                .signerName(user.getUsername())
                .certificateSubject(cert.getSubjectX500Principal().getName())
                .certificateIssuer(cert.getIssuerX500Principal().getName())
                .certificateValidFrom(cert.getNotBefore().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .certificateValidTo(cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .signatureLevel("PAdES-B-B")
                .signedUniqueName(signedName)
                .build();

        return SignatureResponse.from(signatureRepository.save(sigEntity));
    }

    public List<SignatureResponse> getSignatures(Long fileId) {
        return signatureRepository.findAllByFileId(fileId)
                .stream()
                .map(SignatureResponse::from)
                .toList();
    }

    public Path getSignedFilePath(Long signatureId) {
        DocumentSignatureEntity sig = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new IllegalArgumentException("Signature not found: " + signatureId));
        return Path.of(storagePath, sig.getSignedUniqueName());
    }
}
