#!/bin/sh

PROJECT_ROOT=$(git rev-parse --show-toplevel)

# Part 1
stagedFiles=$(git diff --staged --name-only)

# Part 2
echo "Running spotlessApply. Formatting code..."

if [ "$OS" = "Windows_NT" ]; then
    cd "$PROJECT_ROOT" && ./gradlew.bat spotlessApply
else
    cd "$PROJECT_ROOT" && ./gradlew spotlessApply
fi

# Checking the exit status
if [ $? -ne 0 ]; then
    echo "Spotless apply failed!"
    exit 1
fi

# Part 3
for file in $stagedFiles; do
  if test -f "$file"; then
    git add "$file"
  fi
done
