'use client'
import { useEffect } from 'react'
import { useForm, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { usuarioService } from '@/features/auth'
import { ApiError } from '@/shared/types/api'
import { Card, CardContent } from '@/shared/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import { useDraftForm } from '@/shared/hooks/useDraftForm'

const perfilSchema = z.object({
  name: z.string().max(100, 'Maximo 100 caracteres').nullable().optional(),
})

type PerfilFormValues = z.infer<typeof perfilSchema>

const senhaSchema = z.object({
  senhaAtual: z.string().min(1, 'Obrigatorio'),
  novaSenha: z.string().min(6, 'Minimo 6 caracteres'),
})

type SenhaFormValues = z.infer<typeof senhaSchema>

export default function PerfilPage() {
  const queryClient = useQueryClient()

  const {
    data: perfil,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ['perfil'],
    queryFn: usuarioService.getPerfil,
  })

  const perfilForm = useForm<PerfilFormValues>({
    resolver: zodResolver(perfilSchema) as Resolver<PerfilFormValues>,
    defaultValues: { name: '' },
  })

  const { clearDraft, resetWithDraft } = useDraftForm(perfilForm)

  useEffect(() => {
    if (perfil) {
      resetWithDraft({ name: perfil.name ?? '' })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [perfil])

  const senhaForm = useForm<SenhaFormValues>({
    resolver: zodResolver(senhaSchema) as Resolver<SenhaFormValues>,
    defaultValues: { senhaAtual: '', novaSenha: '' },
  })

  const atualizarPerfilMutation = useMutation({
    mutationFn: (values: PerfilFormValues) => {
      const nameTrimmed = (values.name ?? '').trim()
      return usuarioService.atualizarPerfil({
        name: nameTrimmed.length > 0 ? nameTrimmed : null,
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['perfil'] })
      clearDraft()
      toast.success('Perfil atualizado com sucesso.')
    },
    onError: (err: unknown) => {
      const msg = err instanceof ApiError ? err.message : 'Erro ao atualizar perfil.'
      toast.error(msg)
    },
  })

  const alterarSenhaMutation = useMutation({
    mutationFn: (values: SenhaFormValues) =>
      usuarioService.alterarSenha({
        senhaAtual: values.senhaAtual,
        novaSenha: values.novaSenha,
      }),
    onSuccess: () => {
      senhaForm.reset()
      toast.success('Senha alterada com sucesso.')
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError && err.status === 422) {
        toast.error('Senha atual incorreta.')
        return
      }
      const msg = err instanceof ApiError ? err.message : 'Erro ao alterar senha.'
      toast.error(msg)
    },
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <h1 className="text-2xl font-semibold tracking-tight">Meu Perfil</h1>
      </div>

      <div className="max-w-xl space-y-6">
        <Card>
          <CardContent className="pt-6 space-y-4">
            <h2 className="text-lg font-medium">Dados do perfil</h2>
            {isLoading && (
              <p className="text-sm text-muted-foreground">Carregando...</p>
            )}
            {isError && (
              <p className="text-sm text-destructive">
                Erro ao carregar dados do perfil.
              </p>
            )}
            <Form {...perfilForm}>
              <form
                onSubmit={perfilForm.handleSubmit((v) =>
                  atualizarPerfilMutation.mutate(v),
                )}
                className="space-y-4"
              >
                <FormField
                  control={perfilForm.control}
                  name="name"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nome</FormLabel>
                      <FormControl>
                        <Input
                          className="w-full"
                          placeholder="Seu nome"
                          maxLength={100}
                          value={field.value ?? ''}
                          onChange={field.onChange}
                          onBlur={field.onBlur}
                          name={field.name}
                          ref={field.ref}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormItem>
                  <FormLabel>E-mail</FormLabel>
                  <FormControl>
                    <Input
                      className="w-full"
                      value={perfil?.email ?? ''}
                      readOnly
                      disabled
                    />
                  </FormControl>
                </FormItem>

                <div className="pt-2">
                  <Button
                    type="submit"
                    disabled={atualizarPerfilMutation.isPending || isLoading}
                  >
                    {atualizarPerfilMutation.isPending
                      ? 'Salvando...'
                      : 'Salvar'}
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="pt-6 space-y-4">
            <h2 className="text-lg font-medium">Alterar senha</h2>
            <Form {...senhaForm}>
              <form
                onSubmit={senhaForm.handleSubmit((v) =>
                  alterarSenhaMutation.mutate(v),
                )}
                className="space-y-4"
              >
                <FormField
                  control={senhaForm.control}
                  name="senhaAtual"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Senha atual</FormLabel>
                      <FormControl>
                        <Input
                          className="w-full"
                          type="password"
                          autoComplete="current-password"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={senhaForm.control}
                  name="novaSenha"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Nova senha</FormLabel>
                      <FormControl>
                        <Input
                          className="w-full"
                          type="password"
                          autoComplete="new-password"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <div className="pt-2">
                  <Button
                    type="submit"
                    disabled={alterarSenhaMutation.isPending}
                  >
                    {alterarSenhaMutation.isPending
                      ? 'Alterando...'
                      : 'Alterar senha'}
                  </Button>
                </div>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
