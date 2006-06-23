#!/bin/sh

# Bourne shell script to start Cayenne Modeler.
#
# Certain parts are modeled after Tomcat startup scrips, 
# Copyright Apache Software Foundation

MAIN_CLASS=org.objectstyle.cayenne.modeler.Main

CLASSPATH=

# OS specific support.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

PATH_SEPARATOR=:
if [ "$cygwin" = "true" ] ; then 
	PATH_SEPARATOR=";"
fi

# Guess from startup directory
if [ "$CAYENNE_HOME" = "" ] ; then
	# resolve links - $0 may be a softlink
	PRG="$0"

	while [ -h "$PRG" ] ; do
  		ls=`ls -ld "$PRG"`
  		link=`expr "$ls" : '.*-> \(.*\)$'`
  		if expr "$link" : '.*/.*' > /dev/null; then
    		PRG="$link"
  		else
			PRG=`dirname "$PRG"`/"$link"
		fi
	done
 
	CAYENNE_HOME=`dirname "$PRG"`
	CAYENNE_HOME=`dirname "$CAYENNE_HOME"`
fi


if [ ! -f $CAYENNE_HOME/bin/modeler.sh ] ; then
    echo "Please define CAYENNE_HOME to point to your Cayenne installation."
    exit 1
fi

# Guess Java location from PATH or from JAVA_HOME
if [ "$JAVA_HOME" = "" ] ; then 
	JAVACMD=java
else
	JAVACMD=$JAVA_HOME/bin/java
	if [ ! -f $JAVACMD ] ; then
		JAVACMD=$JAVA_HOME/jre/bin/java
	fi
fi

CAYENNE_MODELER_JAR_PATH="${CAYENNE_HOME}/lib/modeler/cayenne-modeler.jar"
if [ "$cygwin" = "true" ] ; then 
    CAYENNE_CLASSPATH=`cygpath -w $CAYENNE_MODELER_JAR_PATH`
else 
    CAYENNE_CLASSPATH=$CAYENNE_MODELER_JAR_PATH
fi
    
OPTIONS="-classpath $CAYENNE_CLASSPATH"

# Mac OS X Specific properties
if [ -d /System/Library/Frameworks/JavaVM.framework ]; then
  PROP1="-Dcom.apple.mrj.application.apple.menu.about.name=CayenneModeler"
  PROP2="-Dapple.laf.useScreenMenuBar=true"
  PROP3="-Xdock:name=CayenneModeler"
  PROP4="-Xdock:icon=${CAYENNE_HOME}/bin/icon.ico"

  PROPERTIES="$PROP1 $PROP2 $PROP3 $PROP4"
fi

# Start the Modeler
$JAVACMD $OPTIONS $PROPERTIES $MAIN_CLASS $@ &
