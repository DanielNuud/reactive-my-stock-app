create table if not exists companies(
    ticker varchar(32) primary key,
    description text,
    name text,
    homepage_url text,
    primary_exchange text,
    market_cap text,
    city text,
    address1 text,
    icon_url text,
    logo_url text,
    status text
    );

create table if not exists tickers(
    ticker varchar(32) primary key,
    company_name text,
    currency varchar(8)
    );