import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ErrorBanner } from './ErrorBanner'
import { useErrorBannerStore } from './error-banner-store'

const sample = {
  codigo: 'ERR-ABCD1234',
  mensagem: 'falha generica',
  tipo: 'TypeError',
  criadoEm: '2026-05-23T10:00:00.000Z',
}

describe('ErrorBanner', () => {
  beforeEach(() => {
    useErrorBannerStore.setState({ banners: [] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('nao renderiza nada quando a lista de banners esta vazia', () => {
    const { container } = render(<ErrorBanner />)
    expect(container).toBeEmptyDOMElement()
  })

  it('renderiza um alerta com tipo, mensagem e codigo', () => {
    useErrorBannerStore.getState().addBanner(sample)

    render(<ErrorBanner />)

    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('TypeError')).toBeInTheDocument()
    expect(screen.getByText('falha generica')).toBeInTheDocument()
    expect(screen.getByText('ERR-ABCD1234')).toBeInTheDocument()
  })

  it('mostra "registrando..." quando codigo e null', () => {
    useErrorBannerStore.getState().addBanner({ ...sample, codigo: null })

    render(<ErrorBanner />)

    expect(screen.getByText('registrando...')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'copiar' }),
    ).not.toBeInTheDocument()
  })

  it('trunca mensagens maiores que 80 caracteres', () => {
    const longa = 'a'.repeat(150)
    useErrorBannerStore.getState().addBanner({ ...sample, mensagem: longa })

    render(<ErrorBanner />)

    expect(screen.getByText(`${'a'.repeat(80)}...`)).toBeInTheDocument()
  })

  it('botao copiar chama navigator.clipboard.writeText com o codigo', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })
    useErrorBannerStore.getState().addBanner(sample)

    render(<ErrorBanner />)

    await userEvent.click(screen.getByRole('button', { name: 'copiar' }))

    expect(writeText).toHaveBeenCalledWith('ERR-ABCD1234')
  })

  it('botao X chama dismiss e remove o banner', async () => {
    useErrorBannerStore.getState().addBanner(sample)

    render(<ErrorBanner />)

    await userEvent.click(screen.getByRole('button', { name: 'Fechar alerta' }))

    expect(useErrorBannerStore.getState().banners).toEqual([])
  })

  it('mostra botao "Fechar todos" quando ha mais de um banner', () => {
    useErrorBannerStore.getState().addBanner({ ...sample, mensagem: 'a' })
    useErrorBannerStore.getState().addBanner({ ...sample, mensagem: 'b' })

    render(<ErrorBanner />)

    expect(
      screen.getByRole('button', { name: 'Fechar todos' }),
    ).toBeInTheDocument()
  })

  it('nao mostra "Fechar todos" com um unico banner', () => {
    useErrorBannerStore.getState().addBanner(sample)

    render(<ErrorBanner />)

    expect(
      screen.queryByRole('button', { name: 'Fechar todos' }),
    ).not.toBeInTheDocument()
  })

  it('"Fechar todos" remove todos os banners', async () => {
    useErrorBannerStore.getState().addBanner({ ...sample, mensagem: 'a' })
    useErrorBannerStore.getState().addBanner({ ...sample, mensagem: 'b' })

    render(<ErrorBanner />)

    await userEvent.click(screen.getByRole('button', { name: 'Fechar todos' }))

    expect(useErrorBannerStore.getState().banners).toEqual([])
  })
})
