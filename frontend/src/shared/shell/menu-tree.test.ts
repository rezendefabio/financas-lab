import { describe, it, expect } from 'vitest'
import { buildMenuTree, findActiveTrail } from './menu-tree'
import type { ScreenDefinition } from './screens.registry'

const screen = (
  code: string,
  path: string,
  menuPath: string[],
): ScreenDefinition => ({
  code,
  title: code,
  path,
  menuPath,
  icon: 'folder',
  permissions: [],
})

describe('buildMenuTree', () => {
  it('agrupa telas em arvore por menuPath', () => {
    const tree = buildMenuTree([
      screen('AAA-AAA-001', '/a', ['Grupo', 'A']),
      screen('AAA-BBB-001', '/b', ['Grupo', 'B']),
    ])
    expect(tree.length).toBe(1)
    expect(tree[0].label).toBe('Grupo')
    expect(tree[0].children.map((c) => c.label)).toEqual(['A', 'B'])
    expect(tree[0].children[0].screen?.code).toBe('AAA-AAA-001')
  })

  it('suporta tres niveis de profundidade', () => {
    const tree = buildMenuTree([
      screen('AAA-AAA-001', '/a', ['N1', 'N2', 'N3']),
    ])
    const n2 = tree[0].children[0]
    const n3 = n2.children[0]
    expect(n3.label).toBe('N3')
    expect(n3.screen?.code).toBe('AAA-AAA-001')
  })

  it('trata folha de nivel 0 (menuPath de um segmento)', () => {
    const tree = buildMenuTree([screen('AAA-AAA-001', '/a', ['Solo'])])
    expect(tree[0].label).toBe('Solo')
    expect(tree[0].screen?.code).toBe('AAA-AAA-001')
    expect(tree[0].children).toEqual([])
  })

  it('trunca menuPath com mais de 3 niveis ao limite', () => {
    const tree = buildMenuTree([
      screen('AAA-AAA-001', '/a', ['N1', 'N2', 'N3', 'N4']),
    ])
    const depth = (node: { children: unknown[] }, d = 1): number =>
      (node.children as { children: unknown[] }[]).length === 0
        ? d
        : Math.max(
            ...(node.children as { children: unknown[] }[]).map((c) =>
              depth(c, d + 1),
            ),
          )
    expect(depth(tree[0])).toBeLessThanOrEqual(3)
  })

  it('gera chaves estaveis e unicas por no', () => {
    const tree = buildMenuTree([
      screen('AAA-AAA-001', '/a', ['Grupo', 'A']),
      screen('AAA-BBB-001', '/b', ['Grupo', 'B']),
    ])
    expect(tree[0].key).toBe('Grupo')
    expect(tree[0].children[0].key).toBe('Grupo/A')
    expect(tree[0].children[1].key).toBe('Grupo/B')
  })
})

describe('findActiveTrail', () => {
  const tree = buildMenuTree([
    screen('AAA-AAA-001', '/a', ['Grupo', 'Sub', 'A']),
    screen('AAA-BBB-001', '/b', ['Grupo', 'Sub', 'B']),
    screen('CCC-CCC-001', '/c', ['Outro', 'C']),
  ])

  it('retorna as chaves de grupo no caminho do item ativo', () => {
    const trail = findActiveTrail(tree, '/a')
    expect(trail.has('Grupo')).toBe(true)
    expect(trail.has('Grupo/Sub')).toBe(true)
    expect(trail.has('Outro')).toBe(false)
  })

  it('retorna conjunto vazio quando nao ha caminho ativo', () => {
    expect(findActiveTrail(tree, undefined).size).toBe(0)
  })

  it('retorna conjunto vazio quando o path nao casa com tela alguma', () => {
    expect(findActiveTrail(tree, '/desconhecido').size).toBe(0)
  })
})
