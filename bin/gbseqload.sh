
# source the configuration file
CONFIG="/home/sc/jsam/gbseqload/gbseqloader.config"
. ${CONFIG}
echo ${JAVARUNTIMEOPTS}
echo ${CLASSPATH}

# run the loader
${JAVA_RUN} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} -DCONFIG=${CONFIG} org.jax.mgi.app.gbseqloader.GBSeqloaderInitial
