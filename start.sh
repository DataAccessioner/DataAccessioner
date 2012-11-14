#!/bin/sh
cd "$(dirname "$0")"
java -Xmx256m -cp $(echo plugins/*.zip | tr ' ' ':') -jar DataAccessioner.jar