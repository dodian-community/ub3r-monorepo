create table if not exists character_stats
(
    uid          int        default 0    not null
        primary key,
    oldid        int        default 0    not null,
    combat       int        default 3    not null,
    total        int        default 29   not null,
    totalxp      bigint     default 1154 not null,
    attack       bigint     default 0    not null,
    defence      bigint     default 0    not null,
    strength     bigint     default 0    not null,
    hitpoints    bigint     default 1154 not null,
    prayer       bigint     default 0    not null,
    magic        bigint     default 0    not null,
    ranged       bigint     default 0    not null,
    woodcutting  bigint     default 0    not null,
    firemaking   bigint     default 0    not null,
    fletching    bigint     default 0    not null,
    mining       bigint     default 0    not null,
    smithing     bigint     default 0    not null,
    agility      bigint     default 0    not null,
    thieving     bigint     default 0    not null,
    runecrafting bigint     default 0    not null,
    slayer       bigint     default 0    not null,
    farming      bigint     default 0    not null,
    crafting     bigint     default 0    not null,
    herblore     bigint     default 0    not null,
    fishing      bigint     default 0    not null,
    cooking      bigint     default 0    not null,
    name         varchar(255)            null,
    rights       tinyint(2) default 0    null,
    INDEX (uid)
)
    engine = InnoDB;

create table if not exists character_stats_progress
(
    id           int auto_increment
        primary key,
    oldid        int        default 0                 not null,
    uid          int        default 0                 not null,
    total        int        default 29                not null,
    totalxp      bigint     default 1200              not null,
    attack       bigint     default 0                 not null,
    defence      bigint     default 0                 not null,
    strength     bigint     default 0                 not null,
    hitpoints    bigint     default 1200              not null,
    combat       int        default 3                 not null,
    magic        bigint     default 0                 not null,
    ranged       bigint     default 0                 not null,
    woodcutting  bigint     default 0                 not null,
    firemaking   bigint     default 0                 not null,
    fletching    bigint     default 0                 not null,
    mining       bigint     default 0                 not null,
    prayer       bigint     default 0                 not null,
    agility      bigint     default 0                 not null,
    thieving     bigint     default 0                 not null,
    runecrafting bigint     default 0                 not null,
    slayer       bigint     default 0                 not null,
    farming      bigint     default 0                 not null,
    crafting     bigint     default 0                 not null,
    herblore     bigint     default 0                 not null,
    fishing      bigint     default 0                 not null,
    cooking      bigint     default 0                 not null,
    smithing     bigint     default 0                 not null,
    rights       tinyint(2) default 0                 null,
    name         varchar(255)                         null,
    updated      datetime   default CURRENT_TIMESTAMP null
)
    engine = InnoDB
    auto_increment = 1;

create table if not exists characters
(
    id              int                  default 0                                            not null
        primary key,
    name            varchar(25)          default ''                                           not null,
    height          tinyint              default 0                                            not null,
    x               smallint             default 2611                                         not null,
    y               smallint             default 3094                                         not null,
    pkrating        smallint             default 0                                            not null,
    lastlogin       varchar(25)          default '0'                                          not null,
    health          smallint             default 10                                           not null,
    look            varchar(55)          default '0 2 13 18 26 33 36 42 3 5 6 3 7 0 1 2 0 0 ' not null,
    slayerData      varchar(55)          default '-1,-1,0,0,0,0,-1'                           not null,
    essence_pouch   varchar(55)          default '0:0:0:0'                                    not null,
    agility         int(2)               default 0                                            not null,
    autocast        int(3)               default -1                                           not null,
    equipment       text                                                                      not null,
    inventory       text                                                                      not null,
    bank            text                                                                      not null,
    friends         text                                                                      not null,
    banned          int(11) unsigned     default 0                                            not null,
    ban_level       int                  default 0                                            not null,
    ban_by          varchar(255)         default ''                                           not null,
    ban_reason      varchar(255)         default ''                                           not null,
    unbantime       bigint(255) unsigned default 0                                            not null,
    muted           tinyint unsigned     default 0                                            not null,
    mute_level      int                  default 0                                            not null,
    mute_by         varchar(255)         default ''                                           not null,
    mute_reason     varchar(255)         default ''                                           not null,
    unmutetime      int(255)             default 0                                            not null,
    mgroup_others   varchar(255)         default ''                                           not null,
    tl_level        int                  default 0                                            not null,
    tradelocked     tinyint(3)           default 0                                            not null,
    tl_reason       varchar(255)         default ''                                           not null,
    tl_by           varchar(255)         default ''                                           not null,
    untradelocktime int(255)             default 0                                            not null,
    mgroup          varchar(255)         default '0'                                          not null,
    lastvote        bigint(255)          default 0                                            not null,
    Boss_Log        text                                                                      null,
    fightStyle      tinyint              default 0                                            not null,
    songUnlocked    text                                                                      null,
    uuid            varchar(255)                                                              null,
    sibling         tinyint(2)           default 0                                            null,
    kc              int                  default 0                                            null,
    dc              int                  default 0                                            null,
    explock         tinyint(2)           default 0                                            null,
    travel          varchar(10)          default '0:0:0:0:0'                                  not null,
    unlocks         varchar(10)          default ''                                           not null,
    news            int                  default 0                                            not null,
    prayer          varchar(255)         default ''                                           not null,
    boosted         varchar(255)         default ''                                           not null,
    INDEX (id, name)
)
    engine = InnoDB;

create table if not exists chat_log
(
    username  varchar(255) null,
    message   varchar(255) null,
    timestamp varchar(255) null
)
    engine = InnoDB;

create table if not exists drop_log
(
    username  varchar(255) null,
    item      int(255)     null,
    amount    int(255)     null,
    type      varchar(255) null,
    timestamp varchar(255) null,
    x         int(10)      null,
    y         int(10)      null,
    z         int(10)      null,
    reason    longtext     null
)
    engine = InnoDB;

create table if not exists duel_log
(
    player        varchar(255) null,
    opponent      varchar(255) null,
    playerstake   varchar(255) null,
    opponentstake varchar(255) null,
    winner        varchar(255) null,
    timestamp     varchar(255) null
)
    engine = InnoDB;

create table if not exists pete_co
(
    Tracker_ID   int      not null
        primary key,
    Name         char(25) null,
    CoinsBillion int      null,
    Coins        int      null,
    ForLater_1   int      null
);

create table if not exists pickup_log
(
    username  varchar(255) null,
    item      int(255)     null,
    amount    int(255)     null,
    type      varchar(255) null,
    timestamp varchar(255) null,
    x         int(10)      null,
    y         int(10)      null,
    z         int(10)      null
)
    engine = InnoDB;

create table if not exists pm_log
(
    sender    varchar(255) null,
    receiver  varchar(255) null,
    message   varchar(255) null,
    timestamp varchar(255) null
)
    engine = InnoDB;

create table if not exists uber3_actions
(
    action varchar(20) not null,
    pid    int         not null
)
    engine = InnoDB;

create table if not exists uber3_command_log
(
    userId int(12)     not null,
    name   varchar(25) null,
    time   text        not null,
    action text        not null
)
    engine = InnoDB;

create table if not exists uber3_doors
(
    id             int auto_increment
        primary key,
    doorX          int(5) null,
    doorY          int(4) null,
    doorID         int(4) null,
    doorFaceOpen   int(2) null,
    doorFaceClosed int(2) null,
    doorFace       int(2) null,
    doorState      int(1) null,
    doorHeight     int(1) null
)
    engine = InnoDB
    charset = utf8
    auto_increment = 1;

create table if not exists uber3_drops
(
    npcid     int           not null,
    percent   double(8, 3)  not null,
    itemid    int           not null,
    amt_min   int default 1 null,
    amt_max   int default 1 null,
    rareShout tinytext      null,
    primary key (npcid, percent, itemid)
)
    engine = InnoDB;

create table if not exists uber3_items
(
    id            int(10)               not null
        primary key,
    name          varchar(50)           null,
    description   longtext              null,
    slot          int(10)               null,
    stackable     tinyint(1)            null,
    tradeable     tinyint(1)            null,
    noteable      tinyint(1)            null,
    shopSellValue mediumint(10)         null,
    shopBuyValue  mediumint(10)         null,
    Alchemy       mediumint(10)         null,
    standAnim     varchar(4)            null,
    walkAnim      varchar(4)            null,
    runAnim       varchar(4)            null,
    attackAnim    varchar(4)            null,
    premium       varchar(1)            null,
    twohanded     tinyint(1) default 0  not null,
    full          tinyint(1) default 0  not null,
    interfaceid   int        default -1 not null,
    Bonus1        int(3)                null,
    Bonus2        int(3)                null,
    Bonus3        int(3)                null,
    Bonus4        int(3)                null,
    Bonus5        int(3)                null,
    Bonus6        int(3)                null,
    Bonus7        int(3)                null,
    Bonus8        int(3)                null,
    Bonus9        int(3)                null,
    Bonus10       int(3)                null,
    Bonus11       int(3)                null,
    Bonus12       int(3)                null
)
    engine = InnoDB
    charset = utf8;

create table if not exists uber3_logs
(
    id     int unsigned not null,
    pid    varchar(45)  not null,
    item   int unsigned not null,
    amount int(255)     not null
)
    engine = InnoDB;

create table if not exists uber3_misc
(
    id      int not null,
    players int not null
)
    engine = InnoDB;

create table if not exists uber3_npcs
(
    id          int auto_increment
        primary key,
    name        varchar(255)     default 'no name' null,
    combat      int              default 0         null,
    attackEmote int              default 806       null,
    deathEmote  int              default 836       null,
    hitpoints   int              default 0         null,
    respawn     int(11) unsigned default 60        null,
    size        int              default 1         null,
    attack      int(5)           default 0         null,
    strength    int(5)           default 0         null,
    defence     int(5)           default 0         null,
    ranged      int(5)           default 0         null,
    magic       int(5)           default 0         null
)
    engine = InnoDB
    auto_increment = 1;

create table if not exists uber3_objects
(
    id   int(4) unsigned null,
    x    int(4) unsigned null,
    y    int(4)          null,
    type int(1)          null
)
    engine = InnoDB
    charset = utf8;

create table if not exists uber3_sessions
(
    id       int auto_increment
        primary key,
    client   int(5) default 1337 null,
    duration int                 null,
    dbid     int                 null,
    hostname varchar(255)        null,
    start    bigint(11)          null,
    end      bigint(11)          null,
    world    int                 null
)
    engine = InnoDB
    auto_increment = 1;

create table if not exists uber3_spawn
(
    id         varchar(8)                  not null,
    x          int(4)                      not null,
    y          int(4)                      not null,
    height     int(4)          default 0   not null,
    rx         int(4)          default 0   null,
    ry         int(4)          default 0   null,
    rx2        int(4)          default 0   null,
    ry2        int(4)          default 0   null,
    movechance int(4) unsigned default 0   null,
    hitpoints  int unsigned                null,
    live       int(4) unsigned default 1   not null,
    face       varchar(45)     default '0' not null,
    primary key (id, x, y, height)
)
    engine = InnoDB
    charset = utf8;

create table if not exists uber3_trades
(
    id   int unsigned auto_increment
        primary key,
    p1   bigint(50) unsigned not null,
    p2   bigint(50) unsigned not null,
    type varchar(45)         not null,
    date mediumtext          null
)
    engine = InnoDB
    auto_increment = 1;

create table if not exists user
(
    userid                      int unsigned auto_increment
        primary key,
    usergroupid                 smallint unsigned   default 0        not null,
    membergroupids              char(250)           default ''       not null,
    displaygroupid              varchar(5)          default '0'      not null,
    username                    varchar(100)        default ''       not null,
    password                    char(32)            default ''       not null,
    salt                        char(30)            default ''       not null,
    email                       char(100)           default ''       not null,
    styleid                     smallint unsigned   default 0        not null,
    parentemail                 char(50)            default ''       not null,
    homepage                    char(100)           default ''       not null,
    passworddate                varchar(255)                         not null,
    aim                         char(20)            default ''       not null,
    yahoo                       char(32)            default ''       not null,
    msn                         char(100)           default ''       not null,
    skype                       char(32)            default ''       not null,
    showvbcode                  smallint unsigned   default 0        not null,
    showbirthday                smallint unsigned   default 2        not null,
    usertitle                   char(250)           default ''       not null,
    customtitle                 smallint            default 0        not null,
    joindate                    int unsigned        default 0        not null,
    daysprune                   smallint            default 0        not null,
    icq                         char(20)            default ''       not null,
    lastactivity                int unsigned        default 0        not null,
    lastpost                    int unsigned        default 0        not null,
    lastpostid                  int unsigned        default 0        not null,
    posts                       int unsigned        default 0        not null,
    reputation                  int                 default 10       not null,
    reputationlevelid           int unsigned        default 1        not null,
    timezoneoffset              char(4)             default ''       not null,
    pmpopup                     smallint            default 0        not null,
    lastvisit                   int unsigned        default 0        not null,
    avatarrevision              int unsigned        default 0        not null,
    profilepicrevision          int unsigned        default 0        not null,
    sigpicrevision              int unsigned        default 0        not null,
    options                     int unsigned        default 33554447 not null,
    birthday                    char(10)            default ''       not null,
    birthday_search             varchar(255)                         not null,
    maxposts                    smallint            default -1       not null,
    startofweek                 smallint            default 1        not null,
    ipaddress                   varchar(255)        default ''       not null,
    referrerid                  int unsigned        default 0        not null,
    languageid                  smallint unsigned   default 0        not null,
    emailstamp                  int unsigned        default 0        not null,
    threadedmode                smallint unsigned   default 0        not null,
    autosubscribe               smallint            default -1       not null,
    pmtotal                     smallint unsigned   default 0        not null,
    pmunread                    smallint unsigned   default 0        not null,
    avatarid                    smallint            default 0        not null,
    ipoints                     int unsigned        default 0        not null,
    infractions                 int unsigned        default 0        not null,
    warnings                    int unsigned        default 0        not null,
    infractiongroupids          varchar(255)        default ''       not null,
    infractiongroupid           smallint unsigned   default 0        not null,
    adminoptions                int unsigned        default 0        not null,
    profilevisits               int unsigned        default 0        not null,
    friendcount                 int unsigned        default 0        not null,
    friendreqcount              int unsigned        default 0        not null,
    vmunreadcount               int unsigned        default 0        not null,
    vmmoderatedcount            int unsigned        default 0        not null,
    socgroupinvitecount         int unsigned        default 0        not null,
    socgroupreqcount            int unsigned        default 0        not null,
    pcunreadcount               int unsigned        default 0        not null,
    pcmoderatedcount            int unsigned        default 0        not null,
    gmmoderatedcount            int unsigned        default 0        not null,
    importuserid                bigint              default 0        not null,
    timespentonline             int(10)             default 0        null,
    post_thanks_user_amount     int unsigned        default 0        not null,
    post_thanks_thanked_posts   int unsigned        default 0        not null,
    post_thanks_thanked_times   int unsigned        default 0        not null,
    recent_thankcnt             int(3)              default 0        not null,
    recent_thankact             tinyint(1)          default 1        not null,
    dbtech_usertag_excluded     tinyint(1) unsigned default 0        not null,
    dbtech_usertag_mentioncount int unsigned        default 0        not null,
    dbtech_usertag_tagcount     int unsigned        default 0        not null,
    dbtech_usertag_mentions     int unsigned        default 0        not null,
    dbtech_usertag_tags         int unsigned        default 0        not null,
    dbtech_usertag_settings     int unsigned        default 0        not null,
    dbtech_usertag_quotecount   int unsigned        default 0        not null,
    dbtech_usertag_hashcount    int unsigned        default 0        not null,
    dbtech_usertag_quotes       int unsigned        default 0        not null,
    enabledTwoFA                tinyint(1)                           null,
    twoFAAccountId              int                                  null,
    INDEX (birthday, showbirthday),
    INDEX (birthday_search),
    INDEX (posts),
    INDEX (referrerid),
    INDEX (usergroupid),
    INDEX (username),
    constraint username_UNIQUE
        unique (username)
)
    engine = InnoDB
    auto_increment = 1000;

create table if not exists worlds
(
    players int unsigned            not null,
    id      varchar(45) default '0' not null
        primary key,
    trade   int                     not null,
    duel    int                     not null,
    pk      int                     not null,
    `drop`  int                     not null
)
    engine = InnoDB;

create table if not exists thread
(
    threadid     int unsigned auto_increment
        primary key,
    title        varchar(250)      default '' not null,
    prefixid     varchar(25)       default '' not null,
    firstpostid  int unsigned      default 0  not null,
    lastpostid   int unsigned      default 0  not null,
    lastpost     int unsigned      default 0  not null,
    forumid      smallint unsigned default 0  not null,
    pollid       int unsigned      default 0  not null,
    open         smallint          default 0  not null,
    replycount   int unsigned      default 0  not null,
    hiddencount  int unsigned      default 0  not null,
    deletedcount int unsigned      default 0  not null,
    postusername varchar(100)      default '' not null,
    postuserid   int unsigned      default 0  not null,
    lastposter   varchar(100)      default '' not null,
    dateline     int unsigned      default 0  not null,
    views        int unsigned      default 0  not null,
    iconid       smallint unsigned default 0  not null,
    notes        varchar(250)      default '' not null,
    visible      smallint          default 0  not null,
    sticky       smallint          default 0  not null,
    votenum      smallint unsigned default 0  not null,
    votetotal    smallint unsigned default 0  not null,
    attach       smallint unsigned default 0  not null,
    similar      varchar(55)       default '' not null,
    taglist      mediumtext                   null
)
    engine = MyISAM;

create index dateline
    on thread (dateline);

create index forumid
    on thread (forumid, visible, sticky, lastpost);

create index lastpost
    on thread (lastpost, forumid);

create index pollid
    on thread (pollid);

create index postuserid
    on thread (postuserid);

create index prefixid
    on thread (prefixid, forumid);

create fulltext index title
    on thread (title);

create table if not exists uber3_refunds
(
    date     datetime          not null
        primary key,
    receiver int               not null,
    item     int               null,
    amount   int               null,
    message  tinyint default 0 null,
    claimed  datetime          null
)
    charset = latin1;

