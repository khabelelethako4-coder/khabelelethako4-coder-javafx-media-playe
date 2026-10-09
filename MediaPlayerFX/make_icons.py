from PIL import Image, ImageDraw
S = 4            # supersampling
N = 128          # final size
W = N * S
WHITE = (255, 255, 255, 255)

def canvas():
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)

def p(*pts):
    return [(x * S, y * S) for x, y in pts]

def save(img, name):
    img.resize((N, N), Image.LANCZOS).save(f"src/main/resources/com/mediaplayer/icons/{name}.png")

def speaker(d):
    d.polygon(p((18, 48), (40, 48), (66, 24), (66, 104), (40, 80), (18, 80)), fill=WHITE)

# play
img, d = canvas(); d.polygon(p((38, 20), (38, 108), (110, 64)), fill=WHITE); save(img, "play")
# pause
img, d = canvas()
d.rounded_rectangle(p((30, 20), (54, 108)), radius=6 * S, fill=WHITE)
d.rounded_rectangle(p((74, 20), (98, 108)), radius=6 * S, fill=WHITE); save(img, "pause")
# stop
img, d = canvas(); d.rounded_rectangle(p((26, 26), (102, 102)), radius=10 * S, fill=WHITE); save(img, "stop")
# previous
img, d = canvas()
d.rectangle(p((18, 22), (30, 106)), fill=WHITE)
d.polygon(p((108, 22), (108, 106), (40, 64)), fill=WHITE); save(img, "prev")
# next
img, d = canvas()
d.rectangle(p((98, 22), (110, 106)), fill=WHITE)
d.polygon(p((20, 22), (20, 106), (88, 64)), fill=WHITE); save(img, "next")
# volume (speaker + waves)
img, d = canvas(); speaker(d)
d.arc(p((60, 40), (92, 88)), -50, 50, fill=WHITE, width=7 * S)
d.arc(p((60, 22), (112, 106)), -50, 50, fill=WHITE, width=7 * S); save(img, "volume")
# muted (speaker + X)
img, d = canvas(); speaker(d)
d.line(p((80, 46), (110, 82)), fill=WHITE, width=8 * S)
d.line(p((110, 46), (80, 82)), fill=WHITE, width=8 * S); save(img, "muted")
# add (plus)
img, d = canvas()
d.rounded_rectangle(p((56, 20), (72, 108)), radius=5 * S, fill=WHITE)
d.rounded_rectangle(p((20, 56), (108, 72)), radius=5 * S, fill=WHITE); save(img, "add")
# remove (minus)
img, d = canvas(); d.rounded_rectangle(p((20, 56), (108, 72)), radius=5 * S, fill=WHITE); save(img, "remove")
# clear (trash can)
img, d = canvas()
d.rounded_rectangle(p((22, 30), (106, 42)), radius=4 * S, fill=WHITE)
d.rounded_rectangle(p((48, 16), (80, 32)), radius=4 * S, fill=WHITE)
d.polygon(p((30, 50), (98, 50), (92, 110), (36, 110)), fill=WHITE)
for x in (50, 64, 78):
    d.line(p((x, 60), (x, 100)), fill=(0, 0, 0, 0), width=5 * S)
save(img, "clear")
