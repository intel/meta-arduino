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
	[ -f $source_tarball ] || (echo "source_tarball: $source_tarball does not exist!"; return 1)

	rm -rf ${S}/* && tar -xvf $source_tarball -C ${S} && rm $source_tarball
	cd ${S}

	#use i586/ for Galileo, i686/ for Edison
	if [ ${MACHINE} = "clanton" ]; then
		target_arch_dir="i586"
	elif [ ${MACHINE} = "edison" ]; then
		target_arch_dir="i686"
	else
		echo "machine: ${MACHINE} unhandled"; return 1;
	fi

	#Windows
	if [ ${SDKMACHINE} = "i686-mingw32" ]; then
		mv sysroots $target_arch_dir
		mv $target_arch_dir/i686-pokysdk-mingw32 $target_arch_dir/pokysdk
		zip -y -r ${SDK_DEPLOY}/${PN}-${SDKMACHINE}.zip $target_arch_dir
	#OSX
	elif [ ${SDKMACHINE} = "i386-darwin" ]; then
		mv sysroots $target_arch_dir
		mv $target_arch_dir/i386-pokysdk-darwin $target_arch_dir/pokysdk

		#do we need files below at all?
		mv environment-setup-* $target_arch_dir/
		mv relocate_sdk.py $target_arch_dir/
		mv site-config-* $target_arch_dir/
		mv version-* $target_arch_dir/

		tar ${SDKTAROPTS} -c --file=${SDK_DEPLOY}/${PN}-${SDKMACHINE}.tar.bz2 .
	#Linux 32 and Linux 64
	elif [ ${SDKMACHINE} = "i586" ] || [ ${SDKMACHINE} = "x86_64" ] || [ ${SDKMACHINE} = "i686" ]; then
		cp ../install_script.sh .
		sed -i "s|DEFAULT_INSTALL_DIR=.*|DEFAULT_INSTALL_DIR="${SDKPATH}"|" install_script.sh

		#change directory structure
		mkdir .$target_arch_dir && mv * .$target_arch_dir && mv .$target_arch_dir $target_arch_dir
		cd $target_arch_dir/sysroots/
		ln -s ${SDKMACHINE}-pokysdk-linux pokysdk
		cd ../..
		tar --owner=root --group=root -j -c --file=${SDK_DEPLOY}/${PN}-${SDKMACHINE}.tar.bz2 .
	else
		echo "sdkmachine: ${SDKMACHINE} unhandled"; return 1;
	fi
}

fakeroot python do_populate_sdk_append() {
    bb.build.exec_func("overwrite_dirs", d)
}
