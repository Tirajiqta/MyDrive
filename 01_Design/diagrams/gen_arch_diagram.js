// Generates a multi-page draw.io class diagram: Controllers -> Services -> Entities
// Each page is laid out to fit on a single A4 (portrait) sheet.
const fs = require('fs');

// ---- helpers -------------------------------------------------------------
const xml = (s) => String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
let idc = 0;
const nid = () => 'n' + (++idc);

const STYLE = {
  controller: 'rounded=0;whiteSpace=wrap;html=1;align=center;verticalAlign=top;fillColor=#dae8fc;strokeColor=#6c8ebf;spacingTop=2;',
  service:    'rounded=0;whiteSpace=wrap;html=1;align=center;verticalAlign=top;fillColor=#d5e8d4;strokeColor=#82b366;spacingTop=2;',
  helper:     'rounded=0;whiteSpace=wrap;html=1;align=center;verticalAlign=top;fillColor=#e1d5e7;strokeColor=#9673a6;spacingTop=2;',
  entity:     'rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;fillColor=#ffe6cc;strokeColor=#d79b00;spacingLeft=6;spacingTop=2;',
  header:     'text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontStyle=1;fontSize=14;',
  title:      'text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontStyle=1;fontSize=18;',
};
const EDGE = 'endArrow=open;html=1;rounded=0;strokeColor=#666666;endSize=8;';
const EDGE_HELP = 'endArrow=open;html=1;rounded=0;dashed=1;strokeColor=#9673a6;endSize=8;';

function classLabel(name, stereotype, members) {
  let s = '';
  if (stereotype) s += '«' + stereotype + '»<br>';
  s += '<b>' + name + '</b>';
  if (members && members.length) s += '<hr size="1">' + members.join('<br>');
  return s;
}

function box(cells, x, y, w, h, style, label) {
  const id = nid();
  cells.push(`        <mxCell id="${id}" value="${xml(label)}" style="${style}" vertex="1" parent="1"><mxGeometry x="${x}" y="${y}" width="${w}" height="${h}" as="geometry"/></mxCell>`);
  return id;
}
function edge(cells, src, tgt, style) {
  const id = nid();
  cells.push(`        <mxCell id="${id}" style="${style}" edge="1" parent="1" source="${src}" target="${tgt}"><mxGeometry relative="1" as="geometry"/></mxCell>`);
}

// ---- entity field data ---------------------------------------------------
const E = {
  UserEntity: ['id: Long','username: String','password: String','email: String','createdAt: LocalDateTime','lastLogin: LocalDateTime','currentStorageUsed: Long','preferredLanguage: LanguageEntity','activeSubscription: UserSubscription'],
  FileEntity: ['id: Long','originalFileName: String','uniqueName: String','type: String','size: Long','uploadDate: LocalDateTime','lastModifiedDate: LocalDateTime','owner: UserEntity','parent: FolderEntity','isDeleted: Boolean'],
  FolderEntity: ['id: Long','canonicalName: String','creationDate: LocalDateTime','lastModifiedDate: LocalDateTime','owner: UserEntity','parent: FolderEntity','isDeleted: Boolean','childrenFolders: Set<FolderEntity>','files: Set<FileEntity>'],
  SharedItemEntity: ['id: Long','itemId: Long','itemType: ItemType','owner: UserEntity','sharedWithUser: UserEntity','permissionLevel: PermissionLevel','sharedDate: LocalDateTime','tokenHash: String','expiresAt: LocalDateTime','revokedAt: LocalDateTime'],
  DocumentContentEntity: ['fileId: Long','content: String','lastUpdated: LocalDateTime','lastEditorUsername: String'],
  RefreshTokenEntity: ['id: Long','user: UserEntity','tokenHash: String','expiresAt: Instant','revokedAt: Instant'],
  PasswordResetTokenEntity: ['id: Long','user: UserEntity','tokenHash: String','expiresAt: Instant','usedAt: Instant'],
  LanguageEntity: ['id: Long','code: String','name: String','isActive: Boolean'],
  UserSubscriptionEntity: ['id: Long','user: UserEntity','plan: PlanEntity','startDate: LocalDateTime','endDate: LocalDateTime','status: String','renewalDate: LocalDateTime','paymentMethodId: String','createdAt: LocalDateTime','updatedAt: LocalDateTime'],
  PlanEntity: ['id: Long','internalName: String','storageLimitGB: Integer','pricePerMonth: BigDecimal','currency: String','isActive: Boolean','createdAt: LocalDateTime','updatedAt: LocalDateTime'],
  PlanTranslationEntity: ['id: Long','plan: PlanEntity','language: LanguageEntity','name: String','description: String'],
  SupportTicketEntity: ['id: Long','user: UserEntity','subject: String','description: String','status: String','priority: String','assignedAgent: UserEntity','createdAt: LocalDateTime','updatedAt: LocalDateTime','githubIssueUrl: String','githubIssueId: Long'],
  ChatbotInteractionEntity: ['id: Long','ticket: SupportTicketEntity','user: UserEntity','message: String','response: String','timestamp: LocalDateTime','isUserMessage: Boolean'],
  DocumentSignatureEntity: ['id: Long','fileId: Long','signedBy: UserEntity','signedAt: LocalDateTime','signerName: String','certificateSubject: String','certificateIssuer: String','certValidFrom: LocalDateTime','certValidTo: LocalDateTime','signatureLevel: String','signedUniqueName: String'],
  FaqEntity: ['id: Long','internalQuestionKey: String','createdAt: LocalDateTime','updatedAt: LocalDateTime','translations: Set<FaqTranslation>'],
  FaqTranslationEntity: ['id: Long','faq: FaqEntity','language: LanguageEntity','question: String','answer: String','category: String','keywords: String'],
};
// compact field set for entities referenced from another domain
const COMPACT = {
  UserEntity: ['id: Long','username: String','email: String','…'],
  FileEntity: ['id: Long','originalFileName: String','owner: UserEntity','…'],
  LanguageEntity: ['id: Long','code: String','name: String'],
  SupportTicketEntity: ['id: Long','subject: String','user: UserEntity','…'],
};

// ---- page definitions ----------------------------------------------------
// services: {name, type:'service'|'helper', entities:[...]}
// controllers: {name, services:[...], entities:[...] (direct repo->entity)}
const pages = [
  {
    name: 'Auth & Users',
    controllers: [
      {name:'AuthController', services:['AuthService','PasswordService']},
    ],
    services: [
      {name:'AuthService',  type:'service', entities:['UserEntity','RefreshTokenEntity','UserSubscriptionEntity','PlanEntity','LanguageEntity'], helpers:['JwtService']},
      {name:'PasswordService', type:'service', entities:['PasswordResetTokenEntity','UserEntity']},
      {name:'UserDetailsServiceImpl', type:'service', entities:['UserEntity']},
      {name:'JwtService', type:'helper', entities:[]},
    ],
    entities: ['UserEntity','UserSubscriptionEntity','PlanEntity','RefreshTokenEntity','PasswordResetTokenEntity','LanguageEntity'],
    full: ['UserEntity','RefreshTokenEntity','PasswordResetTokenEntity','LanguageEntity'],
  },
  {
    name: 'Files, Folders & Sharing',
    controllers: [
      {name:'FileController', services:['FileService']},
      {name:'FolderController', services:['FolderService']},
      {name:'ShareController', services:['ShareService']},
      {name:'PublicShareController', services:['ShareService']},
      {name:'CollaborationController', services:[], directEntities:['DocumentContentEntity']},
      {name:'DocumentContentController', services:[], directEntities:['DocumentContentEntity']},
    ],
    services: [
      {name:'FileService', type:'service', entities:['FileEntity','FolderEntity','UserEntity'], helpers:['StorageService']},
      {name:'FolderService', type:'service', entities:['FolderEntity','UserEntity']},
      {name:'ShareService', type:'service', entities:['SharedItemEntity','FileEntity','FolderEntity','UserEntity']},
      {name:'StorageService', type:'helper', entities:[]},
    ],
    entities: ['FileEntity','FolderEntity','SharedItemEntity','DocumentContentEntity','UserEntity'],
    full: ['FileEntity','FolderEntity','SharedItemEntity','DocumentContentEntity'],
  },
  {
    name: 'Subscriptions, Plans & Support',
    controllers: [
      {name:'SubscriptionController', services:['SubscriptionService']},
      {name:'SupportTicketController', services:['SupportTicketService']},
    ],
    services: [
      {name:'SubscriptionService', type:'service', entities:['UserSubscriptionEntity','PlanEntity','UserEntity']},
      {name:'SupportTicketService', type:'service', entities:['SupportTicketEntity','UserEntity']},
    ],
    entities: ['UserSubscriptionEntity','PlanEntity','PlanTranslationEntity','SupportTicketEntity','UserEntity'],
    full: ['UserSubscriptionEntity','PlanEntity','PlanTranslationEntity','SupportTicketEntity'],
  },
  {
    name: 'Chat, FAQ, Signing & Language',
    controllers: [
      {name:'ChatController', services:['ChatService']},
      {name:'FaqController', services:['FaqService']},
      {name:'DocumentSigningController', services:['DocumentSigningService']},
      {name:'LanguageController', services:[], directEntities:['LanguageEntity']},
    ],
    services: [
      {name:'ChatService', type:'service', entities:['ChatbotInteractionEntity','UserEntity'], helpers:['OpenAIService']},
      {name:'FaqService', type:'service', entities:['FaqEntity','FaqTranslationEntity','LanguageEntity']},
      {name:'DocumentSigningService', type:'service', entities:['DocumentSignatureEntity','FileEntity','UserEntity']},
      {name:'OpenAIService', type:'helper', entities:[]},
    ],
    entities: ['ChatbotInteractionEntity','FaqEntity','FaqTranslationEntity','DocumentSignatureEntity','LanguageEntity','FileEntity','UserEntity'],
    full: ['ChatbotInteractionEntity','FaqEntity','FaqTranslationEntity','DocumentSignatureEntity'],
  },
];

// ---- layout --------------------------------------------------------------
const COLX = { c: 30, s: 310, e: 580 };
const W = { c: 200, s: 220, e: 250 };
const STER_H = 46;        // stereotype box height
const LINE_H = 15;        // per member line
const HEAD_H = 28;        // header above member lines
const TOP = 110;          // first row y
const GAP = 18;

function entityHeight(name, full) {
  const fields = full.includes(name) ? E[name] : (COMPACT[name] || E[name]);
  return HEAD_H + fields.length * LINE_H + 6;
}

function buildPage(p) {
  idc = 0; // restart per page for tidy ids (ids only need uniqueness within model)
  const cells = [];
  // title + column headers
  box(cells, 40, 20, 700, 30, STYLE.title, 'MyDrive – ' + p.name);
  box(cells, COLX.c, 70, W.c, 28, STYLE.header, 'Controllers');
  box(cells, COLX.s, 70, W.s, 28, STYLE.header, 'Services');
  box(cells, COLX.e, 70, W.e, 28, STYLE.header, 'Entities');

  // entities column first (so we have ids to point at)
  const eId = {};
  let y = TOP;
  for (const name of p.entities) {
    const isFull = p.full.includes(name);
    const fields = isFull ? E[name] : (COMPACT[name] || E[name]);
    const h = HEAD_H + fields.length * LINE_H + 6;
    eId[name] = box(cells, COLX.e, y, W.e, h, STYLE.entity, classLabel(name, isFull ? null : null, fields));
    y += h + GAP;
  }

  // services column
  const sId = {};
  y = TOP;
  for (const s of p.services) {
    sId[s.name] = box(cells, COLX.s, y, W.s, STER_H, s.type === 'helper' ? STYLE.helper : STYLE.service,
      classLabel(s.name, s.type === 'helper' ? 'Helper' : 'Service', null));
    y += STER_H + GAP;
  }

  // controllers column
  const cId = {};
  y = TOP;
  for (const c of p.controllers) {
    cId[c.name] = box(cells, COLX.c, y, W.c, STER_H, STYLE.controller, classLabel(c.name, 'RestController', null));
    y += STER_H + GAP;
  }

  // edges: controller -> service
  for (const c of p.controllers) {
    for (const sv of (c.services||[])) if (sId[sv]) edge(cells, cId[c.name], sId[sv], EDGE);
    for (const en of (c.directEntities||[])) if (eId[en]) edge(cells, cId[c.name], eId[en], EDGE);
  }
  // edges: service -> entity & service -> helper
  for (const s of p.services) {
    for (const en of (s.entities||[])) if (eId[en]) edge(cells, sId[s.name], eId[en], EDGE);
    for (const hp of (s.helpers||[])) if (sId[hp]) edge(cells, sId[s.name], sId[hp], EDGE_HELP);
  }

  return cells.join('\n');
}

// ---- assemble file -------------------------------------------------------
let diagrams = '';
for (const p of pages) {
  const pid = 'pg-' + p.name.replace(/[^A-Za-z]/g,'').slice(0,12);
  diagrams += `  <diagram id="${pid}" name="${xml(p.name)}">
    <mxGraphModel dx="900" dy="1100" grid="0" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="850" pageHeight="1100" math="0" shadow="0">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
${buildPage(p)}
      </root>
    </mxGraphModel>
  </diagram>\n`;
}

const out = `<mxfile host="app.diagrams.net" type="device">\n${diagrams}</mxfile>\n`;
fs.writeFileSync(process.argv[2], out, 'utf8');
console.log('written', process.argv[2]);
