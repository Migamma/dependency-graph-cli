#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 groupId:artifactId:version" >&2
    exit 1
fi

IFS=":" read -r group artifact version <<< "$1"
work_dir=$(mktemp -d)
trap 'rm -rf "$work_dir"' EXIT

cat > "$work_dir/pom.xml" <<POM
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>compare</groupId>
    <artifactId>compare</artifactId>
    <version>1.0</version>
    <dependencies>
        <dependency>
            <groupId>$group</groupId>
            <artifactId>$artifact</artifactId>
            <version>$version</version>
        </dependency>
    </dependencies>
</project>
POM

mvn -B -q -f "$work_dir/pom.xml" dependency:tree -DoutputFile="$work_dir/tree.txt"
cat "$work_dir/tree.txt"
