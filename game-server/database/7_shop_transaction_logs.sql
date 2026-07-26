create table if not exists shop_transaction_logs
(
    id             int unsigned auto_increment primary key,
    dbid           int          not null,
    player_name    varchar(255) not null,
    shop_id        int          not null,
    shop_name      varchar(255) null,
    type           varchar(10)  not null,
    item_id        int          not null,
    item_amount    int          not null,
    currency_id    int          not null default 995,
    price_per_item int          not null,
    total_price    bigint       not null,
    x              int          null,
    y              int          null,
    z              int          null,
    timestamp      datetime     not null default current_timestamp,
    index idx_shop_trans_dbid (dbid),
    index idx_shop_trans_shop (shop_id),
    index idx_shop_trans_time (timestamp)
)
    engine = InnoDB;
