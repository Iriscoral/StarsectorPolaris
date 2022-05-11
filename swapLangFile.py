import os


def main():
    print("Auto-search files and swap file names if there is a file which has the same name with specific suffix.")
    print("Modified from Originem by AnyIDElse")

    dir_path = os.path.dirname(os.path.realpath(__file__))
    if os.path.isfile(dir_path + '\mod_info_ENG.json'):
        suffix_to_append = '_CN'
        suffix_to_remove = '_ENG'
    else:
        suffix_to_append = '_ENG'
        suffix_to_remove = '_CN'

    for root, sub_folders, filenames in os.walk(dir_path):
        changed = []

        for sub_folder in sub_folders:
            folder_path = os.path.join(root, sub_folder)
            if folder_path in changed:
                continue
            if sub_folder.find(suffix_to_remove) is not -1:
                original_folder_name = sub_folder.replace(suffix_to_remove, '')
                original_folder_path = os.path.join(root, original_folder_name)
                new_folder_path = os.path.join(root, original_folder_name + suffix_to_append)
                os.rename(original_folder_path, new_folder_path)
                os.rename(folder_path, original_folder_path)

        for file_name in filenames:
            file_path = os.path.join(root, file_name)
            if file_path in changed:
                continue

            (pure_file_name, extension) = os.path.splitext(file_name)
            if pure_file_name.find(suffix_to_remove) is not -1:
                original_file_name = pure_file_name.replace(suffix_to_remove, '')
                original_file_path = os.path.join(root, original_file_name + extension)
                new_file_path = os.path.join(root, original_file_name + suffix_to_append + extension)
                os.rename(original_file_path, new_file_path)
                os.rename(file_path, original_file_path)

                if extension.find('variant') is not -1:
                    lines = open(new_file_path, encoding='utf-8').readlines()
                    os.remove(new_file_path)
                    fp = open(new_file_path, 'w', encoding='utf-8')
                    for s in lines:
                        if s.find('variantId') is not -1:
                            fp.write(s.replace('",', suffix_to_append + '",'))
                        else:
                            fp.write(s)

                    lines = open(original_file_path, encoding='utf-8').readlines()
                    os.remove(original_file_path)
                    fp = open(original_file_path, 'w', encoding='utf-8')
                    for s in lines:
                        fp.write(s.replace(suffix_to_remove, ''))

    print("Lang was changed from " + suffix_to_append.replace('_', '') + " to " + suffix_to_remove.replace('_', ''))
    input("Press Any key To Exit.")


if __name__ == '__main__':
    main()
