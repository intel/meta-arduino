DESCRIPTION = "Meta package for building an Arduino IDE specific toolchain"
LICENSE = "MIT"

PR = "r7"

LIC_FILES_CHKSUM = "file://${COREBASE}/LICENSE;md5=3f40d7994397109285ec7b81fdeb3b58 \
                    file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

inherit populate_sdk

SRC_URI += "file://install_script.sh"

# make sure .tar.bz2 file gets propagated into tmp/deploy/sdk 
# instead of .sh during initial build
SDK_PACKAGING_FUNC = "do_compile"

# This recipe extends standard meta-toolchain recipe by
# taking .tar.bz2 file from tmp/deploy/sdk directory, extracting it
# and modifying paths to meet Arduino IDE standards

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
	elif [ ${SDKMACHINE} = "i386-darwin" ]; then
		mv sysroots i586
		mv i586/i386-pokysdk-darwin i586/pokysdk
		#do we need files below?
		mv environment-setup-* i586/
		mv relocate_sdk.py i586/
		mv site-config-* i586/
		mv version-* i586/
		tar ${SDKTAROPTS} -c --file=${SDK_DEPLOY}/${PN}-${SDKMACHINE}.tar.bz2 .
	elif [ ${SDKMACHINE} = "i586" ]; then
		mv sysroots i586
		sed -i "s/sysroots/i586/g" environment-setup-*
		tar ${SDKTAROPTS} -c --file=${SDK_DEPLOY}/${PN}-${SDKMACHINE}.tar.bz2 .
	fi

	#tar back again
	#cd ${S}
        #tar ${SDKTAROPTS} -c --file=${SDK_DEPLOY}/Arduino-${TOOLCHAIN_OUTPUTNAME}.tar.bz2 .
}

fakeroot python do_populate_sdk_append() {
    #execute bash overwrite_dirs function
    bb.build.exec_func("overwrite_dirs", d)
}
