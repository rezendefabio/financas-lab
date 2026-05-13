'use client'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { Button } from '@/shared/components/ui/button'
import { authService } from '@/features/auth/services/auth.service'
import { useAuth } from '@/features/auth/hooks/use-auth'
import { ApiError } from '@/shared/types/api'

const loginSchema = z.object({
  email: z.string().email('Email invalido'),
  senha: z.string().min(1, 'Senha obrigatoria'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export default function LoginPage() {
  const router = useRouter()
  const auth = useAuth()
  const [error, setError] = useState<string | null>(null)

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', senha: '' },
  })

  async function onSubmit(values: LoginFormValues) {
    setError(null)
    try {
      await authService.login({ email: values.email, senha: values.senha })
      auth.refresh()
      router.push('/')
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError('Email ou senha invalidos')
      } else {
        setError('Erro ao realizar login. Tente novamente.')
      }
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Painel esquerdo -- identidade visual (oculto em mobile) */}
      <div className="hidden lg:flex lg:w-1/2 bg-primary flex-col items-center justify-center p-12">
        <div className="space-y-4 max-w-xs text-center text-primary-foreground">
          <div className="text-5xl font-bold tracking-tight">FL</div>
          <h1 className="text-2xl font-semibold">Financas Lab</h1>
          <p className="text-primary-foreground/70 text-sm leading-relaxed">
            Gestao financeira pessoal. Simples, rapido e seguro.
          </p>
        </div>
      </div>

      {/* Painel direito -- formulario */}
      <div className="flex w-full lg:w-1/2 flex-col items-center justify-center p-8 bg-background">
        <div className="w-full max-w-sm space-y-6">
          {/* Header mobile (visivel apenas em telas pequenas) */}
          <div className="lg:hidden text-center space-y-1">
            <h1 className="text-2xl font-bold">Financas Lab</h1>
          </div>

          <div className="space-y-1">
            <h2 className="text-xl font-semibold tracking-tight">Bem-vindo de volta</h2>
            <p className="text-sm text-muted-foreground">Entre com sua conta para continuar</p>
          </div>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" placeholder="seu@email.com" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="senha"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Senha</FormLabel>
                    <FormControl>
                      <Input type="password" placeholder="••••••••" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              {error && (
                <p className="text-sm text-destructive">{error}</p>
              )}
              <Button
                type="submit"
                className="w-full"
                disabled={form.formState.isSubmitting}
              >
                {form.formState.isSubmitting ? 'Entrando...' : 'Entrar'}
              </Button>
            </form>
          </Form>
        </div>
      </div>
    </div>
  )
}
