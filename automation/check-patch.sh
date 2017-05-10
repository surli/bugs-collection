#!/bin/bash -xe

BUILD_UT=0
RUN_DAO_TESTS=0
BUILD_GWT=0

common_modules_paths=("backend/manager/modules/searchbackend/" \
                      "backend/manager/modules/common/" \
                      "backend/manager/modules/compat/" )

#create a search string with OR on all paths
common_modules_search_string=$(IFS='|'; echo "${common_modules_paths[*]}")



dao_tests_paths=("backend/manager/modules/dal" \
                 "backend/manager/modules/searchbackend/" \
                 "java/org/ovirt/engine/core/common/businessentities" )

#create a search string with OR on all paths
dao_tests_paths_search_string=$(IFS='|'; echo "${dao_tests_paths[*]}")

if git show --pretty="format:" --name-only | egrep -q "\.(xml|java)$"; then
    BUILD_UT=1
fi

if git show --pretty="format:" --name-only | egrep -q \
    "\.(frontend|userportal|webadmin|${common_modules_search_string})$"; then
    BUILD_GWT=1
fi

if git show --pretty="format:" --name-only | egrep \
    "(sql|${dao_tests_paths_search_string})" | \
    egrep -v -q "backend/manager/modules/dal/src/main/resources/bundles"; then
    RUN_DAO_TESTS=1
fi

SUFFIX=".git$(git rev-parse --short HEAD)"

if [ -d /root/.m2/repository/org/ovirt ]; then
    echo "Deleting ovirt folder from maven cache"
    rm -rf /root/.m2/repository/org/ovirt
fi

MAVEN_SETTINGS="/etc/maven/settings.xml"
export BUILD_JAVA_OPTS_MAVEN="\
    -Dgwt.compiler.localWorkers=1 \
"
export EXTRA_BUILD_FLAGS="-gs $MAVEN_SETTINGS"
export BUILD_JAVA_OPTS_GWT="\
    -Xms1G \
    -Xmx4G \
"

# Set the location of the JDK that will be used for compilation:
export JAVA_HOME="${JAVA_HOME:=/usr/lib/jvm/java-1.8.0}"

# Use ovirt mirror if able, fall back to central maven
mkdir -p "${MAVEN_SETTINGS%/*}"
cat >"$MAVEN_SETTINGS" <<EOS
<?xml version="1.0"?>
<settings xmlns="http://maven.apache.org/POM/4.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">

<mirrors>
        <mirror>
                <id>ovirt-maven-repository</id>
                <name>oVirt artifactory proxy</name>
                <url>http://artifactory.ovirt.org/artifactory/ovirt-mirror</url>
                <mirrorOf>*</mirrorOf>
        </mirror>
        <mirror>
                <id>root-maven-repository</id>
                <name>Official maven repo</name>
                <url>http://repo.maven.apache.org/maven2</url>
                <mirrorOf>*</mirrorOf>
        </mirror>
</mirrors>
</settings>
EOS

# Run Dao tests
if [[ $RUN_DAO_TESTS -eq 1 ]]; then
    automation/dao-tests.sh "$EXTRA_BUILD_FLAGS"
fi

# remove any previous artifacts
rm -rf output
rm -f ./*tar.gz
make clean \
    "EXTRA_BUILD_FLAGS=$EXTRA_BUILD_FLAGS"

# execute packaging/setup tests
automation/packaging-setup-tests.sh

# perform quick validations
make validations

# Get the tarball
make dist

# create the src.rpm
rpmbuild \
    -D "_srcrpmdir $PWD/output" \
    -D "_topmdir $PWD/rpmbuild" \
    -D "release_suffix ${SUFFIX}" \
    -D "ovirt_build_extra_flags $EXTRA_BUILD_FLAGS" \
    -D "ovirt_build_quick 1" \
    -ts ./*.gz

# install any build requirements
yum-builddep output/*src.rpm

# create the rpms
# default runs without GWT
RPM_BUILD_MODE="ovirt_build_quick"

if [[ $BUILD_GWT -eq 1 ]]; then
    RPM_BUILD_MODE="ovirt_build_draft"
fi

rpmbuild \
    -D "_rpmdir $PWD/output" \
    -D "_topmdir $PWD/rpmbuild" \
    -D "release_suffix ${SUFFIX}" \
    -D "ovirt_build_ut $BUILD_UT" \
    -D "ovirt_build_extra_flags $EXTRA_BUILD_FLAGS" \
    -D "${RPM_BUILD_MODE} 1" \
    --rebuild output/*.src.rpm

# Move any relevant artifacts to exported-artifacts for the ci system to
# archive
rm -rf exported-artifacts
mkdir -p exported-artifacts
find output -iname \*rpm -exec mv "{}" exported-artifacts/ \;
mv ./*tar.gz exported-artifacts/
