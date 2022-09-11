PROCESS=`jps | grep auto-deploy-2022.7.jar | awk '{print $1}'`
for i in $PROCESS
do
        echo "Kill the $1 process [ $i ]"
        # kill -9 $i
        kill $i
done
