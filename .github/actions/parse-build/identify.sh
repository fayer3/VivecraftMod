# iterates over the built jars, and identifies the loaders
# if a loader is found, it is written to the github output
# valid outputs are
# FABRIC_FILE_NAME
# QUILT_FILE_NAME
# FORGE_FILE_NAME
# NEOFORGE_FILE_NAME

for filename in ./libs/build; do
if [[ "${fileNames}" == *"fabric"* ]]; then
	echo "FABRIC_FILE_NAME=${fileNames}" >> $GITHUB_OUTPUT
elif [[ "${fileNames[i]}" == *"neoforge"* ]]; then
	echo "NEOFORGE_FILE_NAME=${fileNames}" >> $GITHUB_OUTPUT
elif  [[ "${fileNames[i]}" == *"forge"* ]]; then
	echo "FORGE_FILE_NAME=${fileNames}" >> $GITHUB_OUTPUT
elif [[ "${fileNames[i]}" == *"quilt"* ]]; then
	echo "QUILT_FILE_NAME=${fileNames}" >> $GITHUB_OUTPUT
fi
done
