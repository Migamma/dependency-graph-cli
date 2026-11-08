#!/usr/bin/env bash
set -uo pipefail

cd "$(dirname "$0")/.."

JAR="target/dependency-graph-cli-1.0-SNAPSHOT.jar"
CONFIGS="tests/graph-configs"

CASES=(
    "diamond.yaml||0|  A -> B, C"
    "diamond.yaml||0|Packages: 4, dependencies: 4"
    "cycle.yaml||0|  A -> B -> C -> A"
    "cycle.yaml||0|  D -> D"
    "depth-limit.yaml||0|  C -> (not expanded: max depth reached)"
    "depth-limit.yaml||0|Packages: 3, dependencies: 2"
    "filter.yaml||0|Skipped by filter: C"
    "filter.yaml||0|  A -> B"
    "shortcut.yaml||0|  E -> F"
    "undefined-package.yaml||0|  XA -> (no dependencies)"
    "invalid-file.yaml||1|line 2: expected format 'A: B C'"
    "missing-root.yaml||1|Package Z not found in test repository"
    "reverse-diamond.yaml|--reverse|0|  B, A, C"
    "reverse-diamond.yaml|--reverse|0|  D <- B, C"
    "reverse-cycle.yaml|--reverse|0|  C -> A -> B -> C"
    "reverse-depth-limit.yaml|--reverse|0|  C <- (not expanded: max depth reached)"
    "reverse-filter.yaml|--reverse|0|Skipped by filter: B"
    "reverse-undefined.yaml|--reverse|0|Packages depending on XA: 2"
    "reverse-root.yaml|--reverse|0|Packages depending on A: 0"
    "diamond.yaml|--reverse|0|Packages that depend on A:"
)

passed=0
failed=0

for case in "${CASES[@]}"; do
    IFS="|" read -r file flag expected_code expected_text <<< "$case"
    output=$(java -jar "$JAR" $flag "$CONFIGS/$file" 2>&1)
    code=$?
    if [[ "$code" == "$expected_code" && "$output" == *"$expected_text"* ]]; then
        echo "PASS  $flag $file: $expected_text"
        passed=$((passed + 1))
    else
        echo "FAIL  $file: expected '$expected_text' (code $code, output: $output)"
        failed=$((failed + 1))
    fi
done

echo
echo "Passed: $passed, failed: $failed"
[[ "$failed" == 0 ]]
