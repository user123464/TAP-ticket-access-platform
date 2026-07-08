USE master;
GO

-- 檢查資料庫是否已經存在，如果不存在才建立它
IF DB_ID('ProjectDB') IS NULL
BEGIN
    CREATE DATABASE ProjectDB;
    PRINT 'ProjectDB has been created successfully.';
END
ELSE
BEGIN
    -- 如果已經存在，什麼都不做，保護現有資料
    PRINT 'ProjectDB already exists. Bypassing creation to protect data.';
END
GO