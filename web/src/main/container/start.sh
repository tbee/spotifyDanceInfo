# initialize the data directory (--update=none do NOT overwrite an existing file)
cp --update=none --recursive /data-init /data

# start application
cd /data
java -jar /app/*.jar