DESCRIPTION = "Meta package for building an Arduino IDE specific toolchain"
LICENSE = "MIT"

# This recipe extends standard meta-toolchain recipe by
# taking .tar.bz2 file from tmp/deploy/sdk directory, extracting it
# and modifying paths to meet Arduino IDE standards

PR = "r7"

LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit populate_sdk

#that script is extracted 'create_shar' function from populate_sdk_base class
SRC_URI += "file://install_script.sh"

# make sure .tar.bz2 file gets propagated into tmp/deploy/sdk 
# instead of .sh during initial build
SDK_PACKAGING_FUNC = "do_compile"

fakeroot overwrite_dirs() {
	source_tarball=${SDK_DEPLOY}/${TOOLCHAIN_OUTPUTNAME}.tar.bz2

	#check if tarball exists - if not, drop an error
	[ -f $source_tarball ] || (echo "$source_tarball does not exist!"; return 1)

	rm -rf ${S}/* && tar -xvf $source_tarball -C ${S} && rm $source_tarball
	cd ${S}

	#Windows
	if [ ${SDKMACHINE} = "i686-mingw32" ]; then
		mv sysroots i586
		mv i586/i686-pokysdk-mingw32 i586/pokysdk
		zip -y -r ${SDK_DEPLOY}/${PN}-${SDKMACHINE}.zip i586
	#OSX
	elif [ ${SDKMACHINE} = "i386-darwin" ]; then
		mv sysroots i586
		mv i586/i386-pokysdk-darwin i586/pokysdk

		#do we need files below at all?
		mv environment-setup-* i586/
		mv relocate_sdk.py i586/
		mv site-config-* i586/
		mv version-* i586/

		tar ${SDKTAROPTS} -c --file=${SDK_DEPLOY}/${PN}-${SDKMACHINE}.tar.bz2 .
	#Linux 32 and Linux 64
	elif [ ${SDKMACHINE} = "i586" ] || [ ${SDKMACHINE} = "x86_64" ] || [ ${SDKMACHINE} = "i686" ]; then
		cp ../install_script.sh .
		sed -i "s|DEFAULT_INSTALL_DIR=.*|DEFAULT_INSTALL_DIR="${SDKPATH}"|" install_script.sh

		#change directory structure
		mkdir .i586 && mv * .i586 && mv .i586 i586
		cd i586/sysroots/
		ln -s ${SDKMACHINE}-pokysdk-linux pokysdk
		cd ../..
		tar --owner=root --group=root -j -c --file=${SDK_DEPLOY}/${PN}-${SDKMACHINE}.tar.bz2 .
	fi
}

fakeroot python do_populate_sdk_append() {
    bb.build.exec_func("overwrite_dirs", d)
}
