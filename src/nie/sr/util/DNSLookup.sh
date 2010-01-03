#!/bin/sh

java -Djdbc.drivers=oracle.jdbc.driver.OracleDriver nie.sr.util.DNSLookup $1 $2 $3 $4 $5 $6 $7 $8
