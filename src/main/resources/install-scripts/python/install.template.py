import argparse
import json
import os
import sys

from jupyter_client.kernelspec import KernelSpecManager

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Install the @KERNEL_NAME@ kernel.')

    install_location = parser.add_mutually_exclusive_group()
    install_location.add_argument(
        '--user',
        help='Install to the per-user kernel registry.',
        action='store_true'
    )
    install_location.add_argument(
        '--sys-prefix',
        help="Install to Python's sys.prefix. Useful in conda/virtual environments.",
        action='store_true'
    )
    install_location.add_argument(
        '--prefix',
        help='''
        Specify a prefix to install to, e.g. an env.
        The kernelspec will be installed in PREFIX/share/jupyter/kernels/
        ''',
        default=''
    )

    parser.add_argument(
        '--replace',
        help='Replace any existing kernel spec with this name.',
        action='store_true'
    )

    args = parser.parse_args()

    # Install the kernel
    install_dest = KernelSpecManager().install_kernel_spec(
        '@KERNEL_DIRECTORY@',
        kernel_name='@KERNEL_NAME@',
        user=args.user,
        prefix=sys.prefix if args.sys_prefix else args.prefix,
        replace=args.replace
    )

    # Connect the self referencing token left in the kernel.json to point to it's install location.

    # Prepare the token replacement string which should be properly escaped for use in a JSON string
    # The [1:-1] trims the first and last " json.dumps adds for strings.
    install_dest_json_fragment = json.dumps(install_dest)[1:-1]

    # Prepare the paths to the installed kernel.json and the one bundled with this installer.
    local_kernel_json_path = os.path.join('@KERNEL_DIRECTORY@', 'kernel.json')
    installed_kernel_json_path = os.path.join(install_dest, 'kernel.json')

    # Replace the @KERNEL_INSTALL_DIRECTORY@ token with the path to where the kernel was installed
    # in the installed kernel.json from the local template.
    with open(local_kernel_json_path, 'r') as template_kernel_json_file:
        template_kernel_json_contents = template_kernel_json_file.read()
        kernel_json_contents = template_kernel_json_contents.replace(
            '@KERNEL_INSTALL_DIRECTORY@',
            install_dest_json_fragment
        )
        with open(installed_kernel_json_path, 'w') as installed_kernel_json_file:
            installed_kernel_json_file.write(kernel_json_contents)

    print('Installed @KERNEL_NAME@ kernel into "%s"' % install_dest)
