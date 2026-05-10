import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from "react";

interface BaseProps {
  label: string;
  error?: string;
}

type FormInputProps = BaseProps & InputHTMLAttributes<HTMLInputElement>;

export function FormInput({ label, error, id, ...props }: FormInputProps) {
  const inputId = id || String(props.name);
  return (
    <label className="field" htmlFor={inputId}>
      <span>{label}</span>
      <input id={inputId} {...props} />
      {error ? <small className="field-error">{error}</small> : null}
    </label>
  );
}

type SelectProps = BaseProps & SelectHTMLAttributes<HTMLSelectElement>;

export function FormSelect({ label, error, id, children, ...props }: SelectProps) {
  const inputId = id || String(props.name);
  return (
    <label className="field" htmlFor={inputId}>
      <span>{label}</span>
      <select id={inputId} {...props}>
        {children}
      </select>
      {error ? <small className="field-error">{error}</small> : null}
    </label>
  );
}

type TextareaProps = BaseProps & TextareaHTMLAttributes<HTMLTextAreaElement>;

export function FormTextarea({ label, error, id, ...props }: TextareaProps) {
  const inputId = id || String(props.name);
  return (
    <label className="field" htmlFor={inputId}>
      <span>{label}</span>
      <textarea id={inputId} {...props} />
      {error ? <small className="field-error">{error}</small> : null}
    </label>
  );
}
