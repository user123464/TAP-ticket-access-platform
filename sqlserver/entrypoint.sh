#!/bin/bash
echo "Starting SQL Server..."
/opt/mssql/bin/sqlservr &

echo "Waiting for SQL Server..."
until /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SELECT 1" > /dev/null 2>&1
do
    echo "SQL Server not ready..."
    sleep 5
done
echo "SQL Server ready."

# 🌟 關鍵防護：檢查是否已經初始化過
if [ ! -f /var/opt/mssql/data/.initialized ]; then
    echo "First time setup: Executing init.sql..."
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -i /init/init.sql
    
    # 建立一個隱藏檔案作為記號，因為 /var/opt/mssql/data 有被掛載持久化，這個檔案會一直留著
    touch /var/opt/mssql/data/.initialized
    echo "Database initialization completed."
else
    echo "Database is already initialized. Skipping init.sql to protect data."
fi

wait