import os
from PIL import Image

def analyze():
    img_path = r"C:\Users\d5u5ei\AppData\Local\Gemini\antigravity\brain\d90527cf-dc87-480c-8075-6ca6fe67294c\screen_verification.png"
    if not os.path.exists(img_path):
        img_path = r"C:\Users\d5u5ei\.gemini\antigravity\brain\d90527cf-dc87-480c-8075-6ca6fe67294c\screen_verification.png"
    
    print(f"Loading image from {img_path}")
    img = Image.open(img_path)
    w, h = img.size
    print(f"Image dimensions: {w}x{h}")
    
    # 扫描红色像素 (R > 160, G < 70, B < 70)
    red_pixels = []
    for y in range(h):
        for x in range(w):
            r, g, b = img.getpixel((x, y))[:3]
            if r > 160 and g < 70 and b < 70:
                red_pixels.append((x, y))
                
    print(f"Total red pixels found: {len(red_pixels)}")
    
    # 我们知道炮二在右下方区域：X 轴在 900~1200, Y 轴在 1100~1350 之间
    pao_two_pixels = [p for p in red_pixels if 900 <= p[0] <= 1200 and 1100 <= p[1] <= 1350]
    if pao_two_pixels:
        avg_x = sum(p[0] for p in pao_two_pixels) / len(pao_two_pixels)
        avg_y = sum(p[1] for p in pao_two_pixels) / len(pao_two_pixels)
        print(f"炮二 (右侧红炮) 的精确重心: X={avg_x:.2f}, Y={avg_y:.2f} (像素数={len(pao_two_pixels)})")
    else:
        print("未在右下方区域找到红炮的红色像素！")
        
    # 我们也找一下左侧的红炮 (炮八): X 轴在 200~500, Y 轴在 1100~1350 之间
    pao_eight_pixels = [p for p in red_pixels if 200 <= p[0] <= 500 and 1100 <= p[1] <= 1350]
    if pao_eight_pixels:
        avg_x = sum(p[0] for p in pao_eight_pixels) / len(pao_eight_pixels)
        avg_y = sum(p[1] for p in pao_eight_pixels) / len(pao_eight_pixels)
        print(f"炮八 (左侧红炮) 的精确重心: X={avg_x:.2f}, Y={avg_y:.2f} (像素数={len(pao_eight_pixels)})")
        
    # 让我们找一下红方的帅 (中路底线): X 轴在 650~800, Y 轴在 1250~1500 之间
    shuai_pixels = [p for p in red_pixels if 650 <= p[0] <= 800 and 1250 <= p[1] <= 1500]
    if shuai_pixels:
        avg_x = sum(p[0] for p in shuai_pixels) / len(shuai_pixels)
        avg_y = sum(p[1] for p in shuai_pixels) / len(shuai_pixels)
        print(f"帅 (红方老帅) 的精确重心: X={avg_x:.2f}, Y={avg_y:.2f} (像素数={len(shuai_pixels)})")
        
    # 让我们列出所有红色像素的聚类 (简单网格聚类)
    clusters = []
    visited = set()
    for x, y in red_pixels:
        if (x, y) in visited:
            continue
        # BFS/DFS 找到连通红色像素
        q = [(x, y)]
        visited.add((x, y))
        curr_cluster = []
        idx = 0
        while idx < len(q):
            cx, cy = q[idx]
            idx += 1
            curr_cluster.append((cx, cy))
            # 搜索附近 15 像素以内的红色像素
            for nx, ny in red_pixels:
                if (nx, ny) not in visited and abs(nx - cx) <= 15 and abs(ny - cy) <= 15:
                    visited.add((nx, ny))
                    q.append((nx, ny))
        if len(curr_cluster) > 50: # 只有足够大的红色斑块才算棋子字样
            cx = sum(p[0] for p in curr_cluster) / len(curr_cluster)
            cy = sum(p[1] for p in curr_cluster) / len(curr_cluster)
            clusters.append((cx, cy, len(curr_cluster)))
            
    print("\n--- 所有检测到的红色棋子标记重心 ---")
    for i, c in enumerate(sorted(clusters, key=lambda x: (x[1], x[0]))):
        print(f"棋子 {i+1}: X={c[0]:.1f}, Y={c[1]:.1f} (红色像素数={c[2]})")

if __name__ == '__main__':
    analyze()
