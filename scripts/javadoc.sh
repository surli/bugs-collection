#!/bin/bash
# This script was originally written by maxiaohao in the aws-mock GitHub project.
# https://github.com/treelogic-swe/aws-mock/

if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    # Set it up
    git config --global user.email "woodyc40@gmail.com"
    git config --global user.name "AgentTroll"
    mkdir gh-pages

    # We're gonna make this a proper repo
    cd gh-pages
    git init
    cd ..

    # Trident included with the javadoc
    git clone -b bleeding-edge https://github.com/TridentSDK/Trident.git

    # Move the server directory into TridentSDK, else the last javadoc will not display the correct index
    cp -R Trident/src/main/java/net/tridentsdk/server src/main/java/net/tridentsdk/

    mvn clean javadoc:javadoc

    # Push!
    cd gh-pages
    git add .
    git commit -m "Auto-publishing Javadoc from Travis CI"
    git push -fq https://AgentTroll:${DOC_PASS}@github.com/TridentSDK/javadocs.git HEAD:gh-pages > /dev/null
    echo "Published JavaDoc.\n" # Done!
fi
