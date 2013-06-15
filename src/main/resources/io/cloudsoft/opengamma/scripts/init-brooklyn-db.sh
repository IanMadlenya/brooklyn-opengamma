[#ftl]
#!/bin/sh

if [ ! -z "$JAVA_HOME" ]; then
  JAVA=$JAVA_HOME/bin/java
elif [ -x /opt/jdk1.6.0_16/bin/java ]; then
  JAVA=/opt/jdk1.6.0_16/bin/java
else
  # No JAVA_HOME, try to find java in the path
  JAVA=`which java 2>/dev/null`
  if [ ! -x "$JAVA" ]; then
    # No java executable in the path either
    echo "Error: Cannot find a JRE or JDK. Please set JAVA_HOME"
    exit 1
  fi
fi

if [ "`basename $0`" = "init-brooklyn-db.sh" ] ; then
  cd `dirname $0`/.. #PLAT-1527
fi

CLASSPATH=config:og-examples.jar
for FILE in `ls -1 lib/*` ; do
  CLASSPATH=$CLASSPATH:$FILE
done

echo "### Creating empty database"

$JAVA  -cp "$CLASSPATH" \
  -Dlogback.configurationFile=jetty-logback.xml \
  com.opengamma.util.test.DbTool \
  -jdbcUrl jdbc:postgresql://${driver.databaseLocation}/example \
  -database example \
  -user "opengamma" \
  -password "OpenGamma" \
  -drop false \
  -create true \
  -createtables true \
  -dbscriptbasedir .

$JAVA  -cp "$CLASSPATH" \
  -Dlogback.configurationFile=jetty-logback.xml \
  com.opengamma.util.test.DbTool \
  -jdbcUrl jdbc:postgresql://${driver.databaseLocation}/opengamma \
  -database opengamma \
  -user "opengamma" \
  -password "OpenGamma" \
  -drop false \
  -create true \
  -createtables true \
  -dbscriptbasedir .

echo "### Adding example data"

$JAVA  -cp "$CLASSPATH" \
  -Xms512M \
  -Xmx1024M \
  -Dlogback.configurationFile=jetty-logback.xml \
  com.opengamma.examples.tool.ExampleDatabasePopulator

echo "### Completed"

