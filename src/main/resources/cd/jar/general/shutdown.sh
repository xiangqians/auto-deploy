PROCESS=`./jps.sh | grep ${JAR_NAME} | awk '{print $1}'`
for i in $PROCESS
do
        echo "Kill the $1 process [ $i ]"
        kill -9 $i
done
