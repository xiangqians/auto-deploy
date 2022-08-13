sudo docker stop ${NAME};
sudo docker rm ${NAME};
sudo docker rmi ${TAG};
rm -rf ${ABSOLUTE_WORK_DIR}/*;
