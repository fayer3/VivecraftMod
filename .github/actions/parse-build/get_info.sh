# extracts build type and forge/neoforge version range from the source/built jars
# also checks if a dedicates quilt jar is built
# outputs are:
# RELEASE_TYPE
# LOADERS_FABRIC
# LOADERS_QUILT
# LOADERS_FORGE
# FORGE_MC_VERSIONS
# LOADERS_NEOFORGE
# NEOFORGE_MC_VERSIONS

parse_forge () {
	minecraftDep=false
	minecraftIdReg='modId\s+=\s+"minecraft"'
	versionRangeReg='versionRange\s+=\s+"(.+?)"'
	while read -r; do
	if [[ ${REPLY} =~ ${minecraftIdReg} ]]; then
		minecraftDep=true
	elif [[ ${REPLY} =~ ${versionRangeReg} && ${minecraftDep} == "true" ]]; then
		parsedVersion="${BASH_REMATCH[1]}"
	fi
	done < ./$1
}

# save the current IFS value
OLDIFS=${IFS}
# set the IFS to the delimiter ("-")
IFS="-"
# split the string into an array, limiting it to a maximum of 3 fields
array=(${MOD_VERSION})
# restore the IFS value
IFS=${OLDIFS}

release_type="release"
if [ ${array[1]:0:1} = "a" ]; then
	release_type="alpha"
elif [ ${array[1]:0:1} = "b" ]; then
	release_type="beta"
fi

echo "RELEASE_TYPE=${release_type}" >> $GITHUB_OUTPUT

# mod loaders
loaders_fabric=fabric
# check if there is a quilt specific jar
if [ -z "${QUILT_FILE_NAME}" ]; then
	loaders_fabric="${loaders_fabric} quilt"
else
	loaders_quilt="quilt"
fi
loaders_forge="forge"
loaders_neoforge="neoforge"

if [ -n "${FORGE_FILE_NAME}" ]; then
	parse_forge ${FORGE_FILE_NAME} "forge/src/main/resources/META-INF/mods.toml"
	echo "FORGE_MC_VERSIONS="${parsedVersion}"" >> $GITHUB_OUTPUT
fi
if [ -n "${NEOFORGE_FILE_NAME}" ]; then
	parse_forge ${NEOFORGE_FILE_NAME} "neoforge/src/main/resources/META-INF/neoforge.mods.toml"
	echo "NEOFORGE_MC_VERSIONS="${parsedVersion}"" >> $GITHUB_OUTPUT
fi

echo "LOADERS_FABRIC=${loaders_fabric}" >> $GITHUB_OUTPUT
echo "LOADERS_FORGE=${loaders_forge}" >> $GITHUB_OUTPUT
echo "LOADERS_NEOFORGE=${loaders_neoforge}" >> $GITHUB_OUTPUT
echo "LOADERS_QUILT=${loaders_quilt}" >> $GITHUB_OUTPUT