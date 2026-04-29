import json
import os

input_path = 'frontend/src/js/constants/regions_raw.json'
output_path = 'frontend/src/js/constants/regions.js'

with open(input_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# Clean up data
clean_data = {}
for sido, guguns in data.items():
    sido = sido.strip()
    if not sido: continue
    
    clean_guguns = {}
    for gugun, dongs in guguns.items():
        gugun = gugun.strip()
        if not gugun: continue
        
        # Some dongs might be nested inside another dict in the raw data
        if isinstance(dongs, dict):
            # Flatten or skip if empty
            flattened = []
            for sub_k, sub_v in dongs.items():
                if isinstance(sub_v, list):
                    flattened.extend(sub_v)
            clean_guguns[gugun] = sorted(list(set([d.strip() for d in flattened if d.strip()])))
        elif isinstance(dongs, list):
            clean_guguns[gugun] = sorted(list(set([d.strip() for d in dongs if d.strip()])))
            
    if clean_guguns:
        clean_data[sido] = clean_guguns

with open(output_path, 'w', encoding='utf-8-sig') as f:
    f.write('export const REGIONS = ')
    json.dump(clean_data, f, ensure_ascii=False, indent=2)
    f.write(';\n')

print(f"Successfully converted {input_path} to {output_path}")
