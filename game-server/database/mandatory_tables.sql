create table if not exists uber3_misc
(
    id      int not null,
    players int not null
)
    engine = MyISAM;

create table uber3_actions
(
    action varchar(20) not null,
    pid    int         not null
)
    engine = MyISAM;

create table uber3_spawn
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
    engine = MyISAM
    charset = utf8;

create table uber3_npcs
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
    engine = MyISAM;

create table uber3_doors
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
    engine = MyISAM
    charset = utf8;

create table uber3_drops
(
    npcid     int           not null,
    percent   double(8, 3)  not null,
    itemid    int           not null,
    amt_min   int default 1 null,
    amt_max   int default 1 null,
    rareShout tinytext      null,
    primary key (npcid, percent, itemid)
)
    engine = MyISAM;

create table pete_co
(
    Tracker_ID   int      not null
        primary key,
    Name         char(25) null,
    CoinsBillion int      null,
    Coins        int      null,
    ForLater_1   int      null
);

create table uber3_items
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
    engine = MyISAM
    charset = utf8;

create table uber3_objects
(
    id   int(4) unsigned null,
    x    int(4) unsigned null,
    y    int(4)          null,
    type int(1)          null
)
    engine = MyISAM
    charset = utf8;

create table user
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
    constraint username_UNIQUE
        unique (username)
)
    engine = MyISAM;

create index birthday
    on user (birthday, showbirthday);

create index birthday_search
    on user (birthday_search);

create index posts
    on user (posts);

create index referrerid
    on user (referrerid);

create index usergroupid
    on user (usergroupid);

create index username
    on user (username);

