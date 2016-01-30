CREATE DATABASE 'database.fdb' user 'sysdba' password 'masterkey' page_size 8192;

/**
 * Список веб-ресурсов.
 */
CREATE TABLE WEBRESOURCE (
	ID INTEGER PRIMARY KEY, 
	NAME VARCHAR(1024), --название сайта/раздела
	URL VARCHAR(1024) NOT NULL UNIQUE, --URL главной страницы
	KEY_FILE_LOCATION VARCHAR(1024), --имя файла с API-ключом Google
	SERVICE_ACCOUNT_EMAIL VARCHAR(1024), --API аккаунт Google
	USERS_ONLINE INTEGER DEFAULT 0, --число активных пользователей по последним данным
	USERS_ONLINE_PREFERRED INTEGER DEFAULT -1, --Желаемое число активных пользователей
	LAST_UPDATED TIMESTAMP, --последний момент обновления
	UPDATING_PERIOD INTEGER DEFAULT 10, -- период обновления данных о сайте в секундах
	IS_ACTIVE INTEGER DEFAULT 0 --обновлять ли данные об этом сайте (0 -- нет, 1 -- да)
);

CREATE GENERATOR WEBRESOURCE_ID_SEQ;

SET TERM ^ ;

CREATE TRIGGER WEBRESOURCE_ID_GEN FOR WEBRESOURCE
ACTIVE BEFORE INSERT POSITION 1 AS
BEGIN
	IF (NEW.ID IS NULL) THEN NEW.ID = GEN_ID(WEBRESOURCE_ID_SEQ, 1);
	IF (NEW.NAME IS NULL) THEN NEW.NAME = NEW.URL;
END^

SET TERM ; ^
