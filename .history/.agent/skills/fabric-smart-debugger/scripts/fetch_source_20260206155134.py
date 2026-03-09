import os
import sys
import glob
import zipfile
import shutil
import pathlib

def find_loom_cache(start_dir):
    """Finds the .gradle/loom-cache directory starting from the given directory."""
    current_dir = start_dir
    while True:
        loom_cache = os.path.join(current_dir, ".gradle", "loom-cache")
        if os.path.exists(loom_cache):
            return loom_cache
        parent_dir = os.path.dirname(current_dir)
        if parent_dir == current_dir:
            return None
        current_dir = parent_dir

def find_source_jar(loom_cache_dir, class_name):
    """Finds the source jar containing the given class."""
    # Convert class name to path (e.g., net.minecraft.client.Minecraft -> net/minecraft/client/Minecraft.class)
    # We look for the .java file in the source jar
    target_file_path = class_name.replace(".", "/") + ".java"
    
    print(f"Searching for source jar containing: {target_file_path} in {loom_cache_dir}")

    # Recursive search for all *-sources.jar files
    # patterns to check: minecraft sources, remapped mods sources
    
    source_jars = []
    # Use glob to find all sources.jar files recursively
    # This might be slow if there are many files, but loom-cache structure is somewhat predictable
    
    for root, dirs, files in os.walk(loom_cache_dir):
        for file in files:
            if file.endswith("-sources.jar"):
                source_jars.append(os.path.join(root, file))

    print(f"Found {len(source_jars)} source jars to scan.")

    for jar_path in source_jars:
        try:
            with zipfile.ZipFile(jar_path, 'r') as zip_ref:
                # Check if the target file exists in the jar
                # zip_ref.namelist() gives all files, but we can check directly
                if target_file_path in zip_ref.namelist():
                    print(f"Found {target_file_path} in {jar_path}")
                    return jar_path
        except zipfile.BadZipFile:
            print(f"Skipping bad zip file: {jar_path}")
            continue

    return None

def extract_source(jar_path, class_name, output_dir):
    """Extracts the source file for the class from the jar."""
    target_file_path = class_name.replace(".", "/") + ".java"
    
    try:
        with zipfile.ZipFile(jar_path, 'r') as zip_ref:
            extracted_path = zip_ref.extract(target_file_path, path=output_dir)
            # Extracted path will be output_dir/net/minecraft/...
            # We want to return the absolute path to the file
            return os.path.abspath(extracted_path)
    except Exception as e:
        print(f"Error extracting file: {e}")
        return None

def main():
    if len(sys.argv) < 2:
        print("Usage: python fetch_source.py <fully.qualified.ClassName>")
        sys.exit(1)

    class_name = sys.argv[1]
    # Remove .class or .java extension if user provided it
    if class_name.endswith(".class") or class_name.endswith(".java"):
        class_name = class_name[:-5] # remove 5 chars only works for .java, .class is 6. simple fix:
        class_name = os.path.splitext(class_name)[0]

    cwd = os.getcwd()
    loom_cache_dir = find_loom_cache(cwd)
    
    if not loom_cache_dir:
        # Fallback: try default user home gradle text if not in project
        # But user specifically asked for project loom-cache, usually in project dir or ~/.gradle/caches/fabric-loom
        # For this specific user environment, we saw it in e:\mcjavaprog\dontstravemc-template-1.21.10\.gradle\loom-cache
        # So finding it from cwd (project root) should work.
        print("Could not find .gradle/loom-cache directory. Make sure you are running this from the project root.")
        sys.exit(1)

    print(f"Using loom-cache at: {loom_cache_dir}")

    jar_path = find_source_jar(loom_cache_dir, class_name)
    
    if jar_path:
        # Create a temp dir for extraction if it doesn't exist
        temp_dir = os.path.join(loom_cache_dir, "temp_extraction")
        if not os.path.exists(temp_dir):
            os.makedirs(temp_dir)
            
        extracted_file = extract_source(jar_path, class_name, temp_dir)
        if extracted_file:
            print(f"SUCCESS: Source extracted to: {extracted_file}")
            # Identify if it is Minecraft or a Library
            if "minecraft" in os.path.basename(jar_path):
                 print(f"Origin: Minecraft Source")
            else:
                 print(f"Origin: External Library ({os.path.basename(jar_path)})")
        else:
            print("Failed to extract source file.")
            sys.exit(1)
    else:
        print(f"Could not find source jar for class: {class_name}")
        sys.exit(1)

if __name__ == "__main__":
    main()
