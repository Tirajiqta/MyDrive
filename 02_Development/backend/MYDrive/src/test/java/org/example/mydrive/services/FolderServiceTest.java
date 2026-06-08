package org.example.mydrive.services;

import org.example.mydrive.dto.FolderCreateRequest;
import org.example.mydrive.dto.FolderResponse;
import org.example.mydrive.dto.FolderUpdateRequest;
import org.example.mydrive.entities.FolderEntity;
import org.example.mydrive.entities.UserEntity;
import org.example.mydrive.repositories.FolderRepository;
import org.example.mydrive.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

    @Mock private FolderRepository folderRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private FolderService folderService;

    private UserEntity owner;

    @BeforeEach
    void setUp() {
        owner = new UserEntity();
        owner.setId(1L);
        owner.setUsername("john");
        owner.setEmail("john@example.com");
        owner.setCurrentStorageUsed(0L);
    }

    private FolderEntity folder(Long id, FolderEntity parent) {
        FolderEntity f = new FolderEntity();
        f.setId(id);
        f.setOwner(owner);
        f.setParent(parent);
        f.setCanonicalName("folder-" + id);
        f.setIsDeleted(false);
        return f;
    }

    // ----- create -----

    @Test
    void create_rootFolder_savesAndReturnsResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(folderRepository.existsByOwnerIdAndParentIsNullAndCanonicalNameIgnoreCaseAndIsDeletedFalse(1L, "Docs"))
                .thenReturn(false);
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> {
            FolderEntity f = inv.getArgument(0);
            f.setId(100L);
            return f;
        });

        FolderResponse res = folderService.create(1L, new FolderCreateRequest(null, "  Docs  "));

        assertThat(res.id()).isEqualTo(100L);
        assertThat(res.canonicalName()).isEqualTo("Docs"); // trimmed
        assertThat(res.parentId()).isNull();
    }

    @Test
    void create_childFolder_validatesParentOwnership() {
        FolderEntity parent = folder(50L, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(50L, 1L)).thenReturn(Optional.of(parent));
        when(folderRepository.existsByOwnerIdAndParentIdAndCanonicalNameIgnoreCaseAndIsDeletedFalse(1L, 50L, "Sub"))
                .thenReturn(false);
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FolderResponse res = folderService.create(1L, new FolderCreateRequest(50L, "Sub"));

        assertThat(res.parentId()).isEqualTo(50L);
    }

    @Test
    void create_whenUserMissing_throwsUnauthorized() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.create(1L, new FolderCreateRequest(null, "X")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void create_whenParentMissing_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.create(1L, new FolderCreateRequest(99L, "X")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void create_whenDuplicateName_throwsConflict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(folderRepository.existsByOwnerIdAndParentIsNullAndCanonicalNameIgnoreCaseAndIsDeletedFalse(1L, "Docs"))
                .thenReturn(true);

        assertThatThrownBy(() -> folderService.create(1L, new FolderCreateRequest(null, "Docs")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(folderRepository, never()).save(any());
    }

    // ----- list -----

    @Test
    void list_rootFolders_usesParentIsNullQuery() {
        when(folderRepository.findAllByOwnerIdAndParentIsNullAndIsDeletedFalse(1L))
                .thenReturn(List.of(folder(1L, null), folder(2L, null)));

        List<FolderResponse> res = folderService.list(1L, null);

        assertThat(res).hasSize(2);
        verify(folderRepository).findAllByOwnerIdAndParentIsNullAndIsDeletedFalse(1L);
    }

    @Test
    void list_childFolders_usesParentIdQuery() {
        when(folderRepository.findAllByOwnerIdAndParentIdAndIsDeletedFalse(1L, 5L))
                .thenReturn(List.of(folder(6L, folder(5L, null))));

        List<FolderResponse> res = folderService.list(1L, 5L);

        assertThat(res).hasSize(1);
        verify(folderRepository).findAllByOwnerIdAndParentIdAndIsDeletedFalse(1L, 5L);
    }

    // ----- update -----

    @Test
    void update_renamesFolder() {
        FolderEntity f = folder(10L, null);
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));
        when(folderRepository.existsByOwnerIdAndParentIsNullAndCanonicalNameIgnoreCaseAndIsDeletedFalse(1L, "Renamed"))
                .thenReturn(false);
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FolderResponse res = folderService.update(1L, 10L, new FolderUpdateRequest(null, "Renamed"));

        assertThat(res.canonicalName()).isEqualTo("Renamed");
    }

    @Test
    void update_renameToSameNameDifferentCase_isAllowed() {
        FolderEntity f = folder(10L, null);
        f.setCanonicalName("Docs");
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));
        // a folder with that (case-insensitive) name "exists" — itself
        when(folderRepository.existsByOwnerIdAndParentIsNullAndCanonicalNameIgnoreCaseAndIsDeletedFalse(1L, "docs"))
                .thenReturn(true);
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FolderResponse res = folderService.update(1L, 10L, new FolderUpdateRequest(null, "docs"));

        assertThat(res.canonicalName()).isEqualTo("docs");
    }

    @Test
    void update_renameToExistingDifferentFolder_throwsConflict() {
        FolderEntity f = folder(10L, null);
        f.setCanonicalName("Docs");
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));
        when(folderRepository.existsByOwnerIdAndParentIsNullAndCanonicalNameIgnoreCaseAndIsDeletedFalse(1L, "Taken"))
                .thenReturn(true);

        assertThatThrownBy(() -> folderService.update(1L, 10L, new FolderUpdateRequest(null, "Taken")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void update_movesFolderToNewParent() {
        FolderEntity f = folder(10L, null);
        FolderEntity newParent = folder(20L, null);
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(20L, 1L)).thenReturn(Optional.of(newParent));
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        FolderResponse res = folderService.update(1L, 10L, new FolderUpdateRequest(20L, null));

        assertThat(res.parentId()).isEqualTo(20L);
    }

    @Test
    void update_moveIntoItself_throwsBadRequest() {
        FolderEntity f = folder(10L, null);
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> folderService.update(1L, 10L, new FolderUpdateRequest(10L, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void update_moveIntoOwnDescendant_throwsBadRequest() {
        // hierarchy: parent(10) -> child(11)
        FolderEntity parent = folder(10L, null);
        FolderEntity child = folder(11L, parent);
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(parent));
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(11L, 1L)).thenReturn(Optional.of(child));

        // trying to move parent(10) into its descendant child(11)
        assertThatThrownBy(() -> folderService.update(1L, 10L, new FolderUpdateRequest(11L, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getReason()).contains("itself/descendant"));
    }

    @Test
    void update_whenFolderMissing_throwsNotFound() {
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.update(1L, 10L, new FolderUpdateRequest(null, "X")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void update_whenNewParentMissing_throwsNotFound() {
        FolderEntity f = folder(10L, null);
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(77L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.update(1L, 10L, new FolderUpdateRequest(77L, null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ----- delete -----

    @Test
    void delete_softDeletesFolder() {
        FolderEntity f = folder(10L, null);
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(f));
        when(folderRepository.save(any(FolderEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        folderService.delete(1L, 10L);

        assertThat(f.getIsDeleted()).isTrue();
        verify(folderRepository).save(f);
    }

    @Test
    void delete_whenFolderMissing_throwsNotFound() {
        when(folderRepository.findByIdAndOwnerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> folderService.delete(1L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
