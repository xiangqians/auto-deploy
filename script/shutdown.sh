PROCESS=`jps | grep ${pkg.name}.jar | awk '{print $1}'`
for i in $PROCESS
do
        echo "Kill the $1 process [ $i ]"
        # kill -9 $i
        # kill -15 $i
        kill $i
done
