PROCESS=`jps | grep ${project.build.finalName}.jar | awk '{print $1}'`
for i in $PROCESS
do
        echo "Kill the $1 process [ $i ]"
        # kill -2 $i
        # kill -15 $i
        kill $i
done
