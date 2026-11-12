#!/usr/bin/env python3
"""Add independently scrubbable limb animation clips to an existing GLB."""
import argparse
import json
import math
import struct


def quaternion_from_matrix(m):
    # glTF matrices are column-major; preserve the bone's original bind pose.
    sx = math.sqrt(sum(m[i] ** 2 for i in (0, 1, 2)))
    sy = math.sqrt(sum(m[i] ** 2 for i in (4, 5, 6)))
    sz = math.sqrt(sum(m[i] ** 2 for i in (8, 9, 10)))
    a, b, c = m[0] / sx, m[4] / sy, m[8] / sz
    d, e, f = m[1] / sx, m[5] / sy, m[9] / sz
    g, h, i = m[2] / sx, m[6] / sy, m[10] / sz
    trace = a + e + i
    if trace > 0:
        s = math.sqrt(trace + 1) * 2
        q = ((h - f) / s, (c - g) / s, (d - b) / s, s / 4)
    elif a > e and a > i:
        s = math.sqrt(1 + a - e - i) * 2
        q = (s / 4, (b + d) / s, (c + g) / s, (h - f) / s)
    elif e > i:
        s = math.sqrt(1 + e - a - i) * 2
        q = ((b + d) / s, s / 4, (f + h) / s, (c - g) / s)
    else:
        s = math.sqrt(1 + i - a - e) * 2
        q = ((c + g) / s, (f + h) / s, s / 4, (d - b) / s)
    return q, [sx, sy, sz]


def multiply(p, q):
    x, y, z, w = p
    a, b, c, d = q
    return (w*a + x*d + y*c - z*b,
            w*b - x*c + y*d + z*a,
            w*c + x*b - y*a + z*d,
            w*d - x*a - y*b - z*c)


def main(source, target):
    with open(source, 'rb') as stream:
        magic, version, _ = struct.unpack('<III', stream.read(12))
        assert magic == 0x46546C67 and version == 2
        json_length, json_type = struct.unpack('<II', stream.read(8))
        assert json_type == 0x4E4F534A
        doc = json.loads(stream.read(json_length))
        binary_length, binary_type = struct.unpack('<II', stream.read(8))
        assert binary_type == 0x004E4942
        binary = bytearray(stream.read(binary_length))
    assert len(doc.get('skins', [])) == 1 and not doc.get('animations')
    doc['animations'] = []
    motions = [
        ('LeftArmPull', 'LeftArm', 'x', 65),
        ('RightArmPull', 'RightArm', 'x', 65),
        ('LeftLegPull', 'LeftUpLeg', 'z', -50),
        ('RightLegPull', 'RightUpLeg', 'z', -50),
    ]
    for name, bone_short, axis, angle in motions:
        bone_name = 'mixamorig:' + bone_short
        bone = next(index for index, node in enumerate(doc['nodes']) if node.get('name') == bone_name)
        node = doc['nodes'][bone]
        matrix = node.pop('matrix')
        rotation, scale = quaternion_from_matrix(matrix)
        node['translation'] = [matrix[12], matrix[13], matrix[14]]
        node['rotation'] = list(rotation)
        node['scale'] = scale
        half = math.radians(angle) / 2
        delta = ((math.sin(half), 0, 0, math.cos(half)) if axis == 'x'
                 else (0, 0, math.sin(half), math.cos(half)))
        end = multiply(rotation, delta)
        while len(binary) % 4:
            binary.append(0)
        time_offset = len(binary)
        binary += struct.pack('<2f', 0.0, 1.0)
        rotation_offset = len(binary)
        binary += struct.pack('<8f', *(rotation + end))
        time_view = len(doc['bufferViews'])
        doc['bufferViews'].append({'buffer': 0, 'byteOffset': time_offset, 'byteLength': 8})
        rotation_view = len(doc['bufferViews'])
        doc['bufferViews'].append({'buffer': 0, 'byteOffset': rotation_offset, 'byteLength': 32})
        time_accessor = len(doc['accessors'])
        doc['accessors'].append({'bufferView': time_view, 'componentType': 5126,
                                 'count': 2, 'type': 'SCALAR', 'min': [0], 'max': [1]})
        rotation_accessor = len(doc['accessors'])
        doc['accessors'].append({'bufferView': rotation_view, 'componentType': 5126,
                                 'count': 2, 'type': 'VEC4'})
        doc['animations'].append({'name': name,
                                  'samplers': [{'input': time_accessor, 'output': rotation_accessor,
                                                'interpolation': 'LINEAR'}],
                                  'channels': [{'sampler': 0,
                                                'target': {'node': bone, 'path': 'rotation'}}]})
    doc['buffers'][0]['byteLength'] = len(binary)
    json_chunk = json.dumps(doc, separators=(',', ':')).encode()
    json_chunk += b' ' * (-len(json_chunk) % 4)
    binary += b'\0' * (-len(binary) % 4)
    total = 12 + 8 + len(json_chunk) + 8 + len(binary)
    with open(target, 'wb') as stream:
        stream.write(struct.pack('<III', magic, version, total))
        stream.write(struct.pack('<II', len(json_chunk), 0x4E4F534A))
        stream.write(json_chunk)
        stream.write(struct.pack('<II', len(binary), 0x004E4942))
        stream.write(binary)
    print(f'Created {target}: {len(doc["animations"])} clips, {len(binary)} binary bytes')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source')
    parser.add_argument('target')
    args = parser.parse_args()
    main(args.source, args.target)
