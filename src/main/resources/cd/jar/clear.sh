./shutdown.sh;
JS "var files = ${FILES}; \
    for (var i = 0, length = files.length; i < length; i++) { \
        var file = files[i]; \
        var fileName = file.name; \
        out('rm -rf ./' + fileName + ';'); \
    };"
