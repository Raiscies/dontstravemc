import os
import re

def fix_json(dir_path):
    print(f"Checking {dir_path}")
    for root, _, files in os.walk(dir_path):
        for f in files:
            if f.endswith('.json'):
                path = os.path.join(root, f)
                with open(path, 'r', encoding='utf-8') as file:
                    content = file.read()
                
                # Replace "name": with "id":
                new_content = re.sub(r'\"name\"\s*:', '\"id\":', content)
                
                if content != new_content:
                    with open(path, 'w', encoding='utf-8', newline='\n') as file:
                        file.write(new_content)
                    print(f"Updated: {path}")

if __name__ == '__main__':
    fix_json(r'src\main\resources\data\dontstravemc\loot_tables')
