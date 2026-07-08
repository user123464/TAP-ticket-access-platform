FROM mcr.microsoft.com/mssql/server:2025-latest

USER root

COPY init.sql /init/init.sql
COPY entrypoint.sh /init/entrypoint.sh

RUN sed -i 's/\r$//' /init/entrypoint.sh && chmod +x /init/entrypoint.sh

CMD ["/bin/bash", "/init/entrypoint.sh"]