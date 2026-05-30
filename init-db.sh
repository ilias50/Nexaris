#!/bin/bash
set -e

# Fonction pour initialiser une base de donnees et donner les droits a l'utilisateur
setup_database() {
    local db_name=$1
    local app_user=$2
    local app_password=$3

    echo "Initialisation de la base : ${db_name} pour l'utilisateur : ${app_user}"

    mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
        CREATE DATABASE IF NOT EXISTS \`${db_name}\`;
        GRANT ALL PRIVILEGES ON \`${db_name}\`.* TO '${app_user}'@'%';
EOSQL
}

# Verification que les variables essentielles sont bien presentes
if [ -z "${MYSQL_ROOT_PASSWORD}" ] || [ -z "${SPRING_DATASOURCE_USERNAME}" ] || [ -z "${SPRING_DATASOURCE_PASSWORD}" ]; then
    echo "Erreur : Les variables d'environnement MySQL ne sont pas completes."
    exit 1
fi

# On s'assure d'abord que l'utilisateur global existe et a les droits de se connecter
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<-EOSQL
    CREATE USER IF NOT EXISTS '${SPRING_DATASOURCE_USERNAME}'@'%' IDENTIFIED BY '${SPRING_DATASOURCE_PASSWORD}';
EOSQL

# Execution pour chacune de tes bases definies dans ton .env
setup_database "${AUTH_DB_NAME}" "${SPRING_DATASOURCE_USERNAME}" "${SPRING_DATASOURCE_PASSWORD}"
setup_database "${ORG_DB_NAME}" "${SPRING_DATASOURCE_USERNAME}" "${SPRING_DATASOURCE_PASSWORD}"
setup_database "${PLANNING_DB_NAME}" "${SPRING_DATASOURCE_USERNAME}" "${SPRING_DATASOURCE_PASSWORD}"
setup_database "${HOLIDAY_PROXY_DB_NAME}" "${SPRING_DATASOURCE_USERNAME}" "${SPRING_DATASOURCE_PASSWORD}"
setup_database "${NOTIFICATION_DB_NAME}" "${SPRING_DATASOURCE_USERNAME}" "${SPRING_DATASOURCE_PASSWORD}"

# Application des privileges
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "FLUSH PRIVILEGES;"
echo "Toutes les bases Nexaris ont ete configurees avec succes !"