import type { Meta, StoryObj } from '@storybook/nextjs-vite'
import { Button } from './button'

const meta: Meta<typeof Button> = {
  component: Button,
  title: 'UI/Button',
}
export default meta

type Story = StoryObj<typeof Button>

export const Primary: Story = { args: { children: 'Confirmar' } }
export const Destructive: Story = { args: { children: 'Excluir', variant: 'destructive' } }
export const Outline: Story = { args: { children: 'Cancelar', variant: 'outline' } }
