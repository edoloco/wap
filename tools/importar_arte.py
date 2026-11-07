#!/usr/bin/env python3
"""Convierte el dibujo del usuario (SVG de la plantilla, p. ej. arte/de_pie_1.svg) en
app/src/main/resources/com/wap/render/arte.txt: por capa, sus formas en coordenadas locales del hueso.
Uso: python3 tools/importar_arte.py arte/de_pie_1.svg molde.json salida.txt
molde.json sale de tools/Molde.java (matriz de cada hueso en esa pose)."""
import sys, json, re, math
import xml.etree.ElementTree as ET
from svgelements import Path, Matrix, Rect, Circle, Ellipse, Polygon, Polyline, SimpleLine, Line, Close, Move

INK = '{http://www.inkscape.org/namespaces/inkscape}'
def style_of(el, inherit):
    st = dict(inherit)
    for k in ('fill', 'stroke', 'stroke-width', 'fill-rule', 'fill-opacity', 'opacity', 'display', 'stroke-opacity'):
        if el.get(k) is not None: st[k] = el.get(k)
    for part in (el.get('style') or '').split(';'):
        if ':' in part:
            k, v = part.split(':', 1); st[k.strip()] = v.strip()
    return st
def color(v, op):
    if v is None or v == 'none' or not v.startswith('#'): return 0
    v = v[1:]
    if len(v) == 3: v = ''.join(c * 2 for c in v)
    return (int(round(op * 255)) << 24) | int(v, 16)
def flatten(shape):
    """Contornos como listas de puntos (curvas muestreadas)."""
    polys, cur = [], []
    for seg in Path(shape).segments():
        if isinstance(seg, Move):
            if len(cur) > 2: polys.append(cur)
            cur = [complex(seg.end.x, seg.end.y)]; continue
        if isinstance(seg, Close): 
            if len(cur) > 2: polys.append(cur)
            cur = []; continue
        if isinstance(seg, (Line,)):
            cur.append(complex(seg.end.x, seg.end.y)); continue
        n = 6
        for i in range(1, n + 1):
            p = seg.point(i / n); cur.append(complex(p.x, p.y))
    if len(cur) > 2: polys.append(cur)
    return polys

def main(svg, molde, out):
    M = json.load(open(molde)); root = ET.parse(svg).getroot(); lines = ['# arte del personaje: importado de ' + svg.split('/')[-1]]
    def walk(el, mat, st, layer, acc):
        for c in el:
            tag = c.tag.split('}')[1]
            m2 = mat * Matrix(c.get('transform')) if c.get('transform') else Matrix(mat)
            # en svgelements, a*b aplica a y luego b: el transform del hijo va primero
            m2 = Matrix(c.get('transform')) * mat if c.get('transform') else Matrix(mat)
            s2 = style_of(c, st)
            if s2.get('display') == 'none': continue
            if tag == 'g':
                lab = c.get(INK + 'label')
                if c.get(INK + 'groupmode') == 'layer': walk(c, m2, s2, lab, acc.setdefault(lab, []))
                else: walk(c, m2, s2, layer, acc)
                continue
            if layer is None: continue
            f = lambda k: float(c.get(k) or 0)
            if tag == 'path': sh = Path(c.get('d'))
            elif tag == 'rect': sh = Rect(f('x'), f('y'), f('width'), f('height'), rx=f('rx') or f('ry'), ry=f('ry') or f('rx'))
            elif tag == 'circle': sh = Circle(f('cx'), f('cy'), f('r'))
            elif tag == 'ellipse': sh = Ellipse(f('cx'), f('cy'), f('rx'), f('ry'))
            elif tag in ('polygon', 'polyline'): sh = Polygon(c.get('points'))
            elif tag == 'line': sh = Polyline((f('x1'), f('y1')), (f('x2'), f('y2')))
            else: continue
            sh = Path(sh) * m2
            op = float(s2.get('opacity', 1)) * float(s2.get('fill-opacity', 1))
            fill = color(s2.get('fill', '#000000'), op)
            stroke = color(s2.get('stroke'), float(s2.get('opacity', 1)) * float(s2.get('stroke-opacity', 1)))
            sw = float(re.sub('[^0-9.]', '', s2.get('stroke-width', '1')) or 1) * math.sqrt(abs(m2.a * m2.d - m2.b * m2.c))
            if stroke == 0: sw = 0
            if fill == 0 and stroke == 0: continue
            polys = flatten(sh)
            if polys: acc.append((polys, fill, stroke, sw, s2.get('fill-rule') == 'evenodd'))
    acc = {}
    walk(root, Matrix(), {}, None, acc)
    total = 0
    for lab in acc:
        if lab != 'guia' and lab not in M: print('capa sin hueso conocido, se omite:', lab)
    # si una capa de un lado quedó vacía y su par tiene piezas que en realidad están en ese lado, se reparten
    def twin(l):
        for a, b in (('_cercana', '_lejana'), ('_cercano', '_lejano'), ('_lejana', '_cercana'), ('_lejano', '_cercano')):
            if l.endswith(a): return l[:-len(a)] + b
    for lab in list(acc):
        o = twin(lab)
        if o in M and lab in M and not acc.get(o):
            keep, move = [], []
            for sh in acc[lab]:
                pts = [p for q in sh[0] for p in q]; cx = sum(p.real for p in pts) / len(pts); cy = sum(p.imag for p in pts) / len(pts)
                d = lambda L: min((b[6] - cx) ** 2 + (b[7] - cy) ** 2 for b in M[L])
                (move if d(o) < d(lab) else keep).append(sh)
            if move: acc[lab] = keep; acc[o] = move; print(len(move), 'pieza(s) de', lab, 'pasan a', o)
    for lab, shapes in acc.items():
        if lab == 'guia' or lab not in M: continue
        bones = M[lab]; lines.append('L ' + lab)
        for polys, fill, stroke, sw, eo in shapes:
            pts = [p for q in polys for p in q]; cx = sum(p.real for p in pts) / len(pts); cy = sum(p.imag for p in pts) / len(pts)
            j = min(range(len(bones)), key=lambda k: (bones[k][6] - cx) ** 2 + (bones[k][7] - cy) ** 2)
            a, b, c, d, e, f = bones[j][:6]  # AffineTransform: x' = a x + c y + e ; y' = b x + d y + f
            det = a * d - b * c
            def loc(p):
                x, y = p.real - e, p.imag - f
                return ((d * x - c * y) / det, (-b * x + a * y) / det)
            lines.append('S %d %08X %08X %.3f %d' % (j, fill & 0xFFFFFFFF, stroke & 0xFFFFFFFF, sw, 1 if eo else 0))
            for q in polys:
                out_pts = []
                for p in q:
                    x, y = loc(p); out_pts.append('%.2f %.2f' % (x, y))
                lines.append('P ' + ' '.join(out_pts).replace('-0.00', '0.00'))
            total += 1
    for lab in M:
        if lab not in acc: print('capa sin dibujo (queda la original):', lab)
    open(out, 'w').write('\n'.join(lines) + '\n')
    print(total, 'formas en', len([l for l in lines if l.startswith('L ')]), 'capas ->', out)

if __name__ == '__main__': main(*sys.argv[1:4])
