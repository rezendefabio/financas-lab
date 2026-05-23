import { describe, it, expect, beforeEach } from 'vitest'
import { useErrorBannerStore } from './error-banner-store'

const baseItem = {
  codigo: null as string | null,
  mensagem: 'erro generico',
  tipo: 'TypeError',
  criadoEm: '2026-05-23T10:00:00.000Z',
}

describe('useErrorBannerStore', () => {
  beforeEach(() => {
    useErrorBannerStore.setState({ banners: [] })
  })

  it('inicia com banners vazio', () => {
    expect(useErrorBannerStore.getState().banners).toEqual([])
  })

  it('addBanner adiciona um banner com id gerado', () => {
    useErrorBannerStore.getState().addBanner(baseItem)
    const banners = useErrorBannerStore.getState().banners
    expect(banners).toHaveLength(1)
    expect(banners[0].id).toBeTruthy()
    expect(banners[0].mensagem).toBe('erro generico')
    expect(banners[0].codigo).toBeNull()
  })

  it('addBanner limita a 3 banners simultaneos descartando o mais antigo', () => {
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'a' })
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'b' })
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'c' })
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'd' })

    const banners = useErrorBannerStore.getState().banners
    expect(banners).toHaveLength(3)
    expect(banners.map((b) => b.mensagem)).toEqual(['b', 'c', 'd'])
  })

  it('updateCodigo atualiza o codigo do banner correspondente', () => {
    useErrorBannerStore.getState().addBanner(baseItem)
    const id = useErrorBannerStore.getState().banners[0].id

    useErrorBannerStore.getState().updateCodigo(id, 'ERR-ABCD1234')

    expect(useErrorBannerStore.getState().banners[0].codigo).toBe('ERR-ABCD1234')
  })

  it('updateCodigo ignora id desconhecido sem mutar o estado', () => {
    useErrorBannerStore.getState().addBanner(baseItem)
    const antes = useErrorBannerStore.getState().banners
    useErrorBannerStore.getState().updateCodigo('id-inexistente', 'ERR-XX')
    expect(useErrorBannerStore.getState().banners).toEqual(antes)
  })

  it('dismiss remove apenas o banner com o id informado', () => {
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'a' })
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'b' })

    const [first] = useErrorBannerStore.getState().banners
    useErrorBannerStore.getState().dismiss(first.id)

    const banners = useErrorBannerStore.getState().banners
    expect(banners).toHaveLength(1)
    expect(banners[0].mensagem).toBe('b')
  })

  it('dismissAll esvazia a lista de banners', () => {
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'a' })
    useErrorBannerStore.getState().addBanner({ ...baseItem, mensagem: 'b' })

    useErrorBannerStore.getState().dismissAll()

    expect(useErrorBannerStore.getState().banners).toEqual([])
  })
})
