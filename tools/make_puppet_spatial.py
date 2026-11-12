#!/usr/bin/env python3
"""Add two independently scrubbable shoulder/hip axes to a rigged GLB."""
import argparse
import json
import math
import struct


def matrix_quaternion(matrix):
    sx = math.sqrt(sum(matrix[i] ** 2 for i in (0, 1, 2)))
    sy = math.sqrt(sum(matrix[i] ** 2 for i in (4, 5, 6)))
    sz = math.sqrt(sum(matrix[i] ** 2 for i in (8, 9, 10)))
    a, b, c = matrix[0] / sx, matrix[4] / sy, matrix[8] / sz
    d, e, f = matrix[1] / sx, matrix[5] / sy, matrix[9] / sz
    g, h, i = matrix[2] / sx, matrix[6] / sy, matrix[10] / sz
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


def axis_quaternion(axis, degrees):
    half = math.radians(degrees) / 2
    s, c = math.sin(half), math.cos(half)
    return {'x': (s, 0, 0, c), 'y': (0, s, 0, c),
            'z': (0, 0, s, c)}[axis]


def matrix_multiply(a, b):
    return [sum(a[k * 4 + row] * b[column * 4 + k] for k in range(4))
            for column in range(4) for row in range(4)]


def add_clip(doc, binary, name, node_index, base, axis, limit):
    while len(binary) % 4:
        binary.append(0)
    times_offset = len(binary)
    binary += struct.pack('<3f', 0.0, 1.0, 2.0)
    rotations_offset = len(binary)
    values = [multiply(base, axis_quaternion(axis, degrees))
              for degrees in (-limit, 0, limit)]
    binary += struct.pack('<12f', *(v for q in values for v in q))
    times_view = len(doc['bufferViews'])
    doc['bufferViews'].append({'buffer': 0, 'byteOffset': times_offset, 'byteLength': 12})
    rotations_view = len(doc['bufferViews'])
    doc['bufferViews'].append({'buffer': 0, 'byteOffset': rotations_offset, 'byteLength': 48})
    times_accessor = len(doc['accessors'])
    doc['accessors'].append({'bufferView': times_view, 'componentType': 5126,
                             'count': 3, 'type': 'SCALAR', 'min': [0], 'max': [2]})
    rotations_accessor = len(doc['accessors'])
    doc['accessors'].append({'bufferView': rotations_view, 'componentType': 5126,
                             'count': 3, 'type': 'VEC4'})
    doc['animations'].append({'name': name,
        'samplers': [{'input': times_accessor, 'output': rotations_accessor,
                      'interpolation': 'LINEAR'}],
        'channels': [{'sampler': 0, 'target': {'node': node_index, 'path': 'rotation'}}]})


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
    nodes = doc['nodes']
    skin = doc['skins'][0]
    old_ibm_accessor = doc['accessors'][skin['inverseBindMatrices']]
    old_ibm_view = doc['bufferViews'][old_ibm_accessor['bufferView']]
    assert old_ibm_accessor['type'] == 'MAT4' and old_ibm_accessor['componentType'] == 5126
    assert old_ibm_accessor['count'] == len(skin['joints'])
    old_ibm_start = old_ibm_view.get('byteOffset', 0) + old_ibm_accessor.get('byteOffset', 0)
    old_ibm = bytes(binary[old_ibm_start:old_ibm_start + len(skin['joints']) * 64])
    helper_ibms = []
    parents = {child: index for index, node in enumerate(nodes)
               for child in node.get('children', [])}
    doc['animations'] = []
    limbs = (
        ('LeftArm', 'x', 110, 'z', 80, 'LeftForeArm', 'x', 120),
        ('RightArm', 'x', 110, 'z', 80, 'RightForeArm', 'z', 120),
        ('LeftUpLeg', 'z', 55, 'x', 40, 'LeftLeg', 'z', 110),
        ('RightUpLeg', 'z', 55, 'x', 40, 'RightLeg', 'z', 110),
    )
    for name, primary_axis, primary_limit, secondary_axis, secondary_limit, flex_name, flex_axis, flex_limit in limbs:
        bone = next(i for i, node in enumerate(nodes)
                    if node.get('name') == 'mixamorig:' + name)
        parent = parents[bone]
        node = nodes[bone]
        matrix = node.pop('matrix')
        bind_rotation, scale = matrix_quaternion(matrix)
        translation = [matrix[12], matrix[13], matrix[14]]
        node['translation'] = [0, 0, 0]
        node['rotation'] = list(bind_rotation)
        node['scale'] = scale
        joint_position = skin['joints'].index(bone)
        bone_ibm = struct.unpack_from('<16f', old_ibm, joint_position * 64)
        bone_local_without_translation = list(matrix)
        bone_local_without_translation[12:15] = [0, 0, 0]
        helper_ibms.append(matrix_multiply(bone_local_without_translation, bone_ibm))
        helper = len(nodes)
        nodes.append({'name': 'PuppetControl:' + name,
                      'translation': translation, 'rotation': [0, 0, 0, 1],
                      'children': [bone]})
        children = nodes[parent]['children']
        children[children.index(bone)] = helper
        skin['joints'].append(helper)
        add_clip(doc, binary, name + 'Swing', bone, bind_rotation,
                 primary_axis, primary_limit)
        add_clip(doc, binary, name + 'Spread', helper, (0, 0, 0, 1),
                 secondary_axis, secondary_limit)
        flex_bone = next(i for i, child in enumerate(nodes)
                         if child.get('name') == 'mixamorig:' + flex_name)
        flex_node = nodes[flex_bone]
        flex_matrix = flex_node.pop('matrix')
        flex_rotation, flex_scale = matrix_quaternion(flex_matrix)
        flex_node['translation'] = [flex_matrix[12], flex_matrix[13], flex_matrix[14]]
        flex_node['rotation'] = list(flex_rotation)
        flex_node['scale'] = flex_scale
        add_clip(doc, binary, name + 'Flex', flex_bone, flex_rotation,
                 flex_axis, flex_limit)
    while len(binary) % 4:
        binary.append(0)
    ibm_offset = len(binary)
    binary += old_ibm
    for matrix in helper_ibms:
        binary += struct.pack('<16f', *matrix)
    ibm_view = len(doc['bufferViews'])
    doc['bufferViews'].append({'buffer': 0, 'byteOffset': ibm_offset,
                               'byteLength': len(skin['joints']) * 64})
    old_ibm_accessor['bufferView'] = ibm_view
    old_ibm_accessor['byteOffset'] = 0
    old_ibm_accessor['count'] = len(skin['joints'])
    old_ibm_accessor.pop('min', None)
    old_ibm_accessor.pop('max', None)
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
    print(f'{target}: {len(nodes)} nodes, {len(doc["animations"])} clips')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source')
    parser.add_argument('target')
    args = parser.parse_args()
    main(args.source, args.target)
