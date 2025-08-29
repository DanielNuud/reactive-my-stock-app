CREATE TABLE IF NOT EXISTS public.articles (
                                               id                      bigserial PRIMARY KEY,
                                               title                   text        NOT NULL,
                                               author                  text,
                                               description             text,
                                               article_url             text        NOT NULL,
                                               image_url               text,
                                               published_utc           timestamptz NOT NULL,
                                               publisher_name          text,
                                               publisher_logo_url      text,
                                               publisher_homepage_url  text,
                                               publisher_favicon_url   text,
                                               tickers                 text[]      NOT NULL DEFAULT '{}'
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_articles_article_url
    ON public.articles (article_url);
