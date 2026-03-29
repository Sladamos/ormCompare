import json
import glob
import os
import re
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

PROCESS_ALL = True
N_LATEST = 3
ADD_JIT = False

JSON_DIR = './jsons'
PLOT_DIR = './plots'

ORM_NAMES = {
    'hibernate': 'Hibernate',
    'eclipselink': 'EclipseLink',
    'datanucleus': 'DataNucleus',
    'cayenne': 'Cayenne'
}

ORM_ORDER = ['Hibernate', 'EclipseLink', 'DataNucleus', 'Cayenne']


def parse_filename(filepath):
    match = re.match(r'jmh-result-(.+)-(\d+)\.json', os.path.basename(filepath))
    return (match.group(1), match.group(2)) if match else ("unknown_db", "0")


def load_data(filepath):
    data_rows = []
    found_params = set()
    with open(filepath, 'r', encoding='utf-8') as f:
        for entry in json.load(f):
            sec = entry.get('secondaryMetrics', {})
            gc_c = sec.get('gc.count', {}).get('score', 0)
            gc_t = sec.get('gc.time', {}).get('score', 0)
            comp_t = sec.get('comp.time', {}).get('score', 0)

            params = entry.get('params', {})
            raw_orm = params.get('ormProvider', 'unknown')
            pretty_orm = ORM_NAMES.get(raw_orm.lower(), raw_orm.capitalize())

            method_name = entry.get('benchmark', '').split('.')[-1]

            extra_params = [f"{k}: {v}" for k, v in params.items() if k != 'ormProvider']
            if extra_params:
                method_name = f"{method_name}\n({', '.join(extra_params)})"
                for p in extra_params:
                    found_params.add(p)

            data_rows.append({
                'Method': method_name,
                'ORM': pretty_orm,
                'Czas [ms/op]': entry.get('primaryMetric', {}).get('score', 0),
                'Alokacja [MB/op]': sec.get('gc.alloc.rate.norm', {}).get('score', 0) / (1024 * 1024),
                'GC Count': gc_c if not pd.isna(gc_c) else 0,
                'GC Time [ms]': gc_t if not pd.isna(gc_t) else 0,
                'JIT Time [ms]': comp_t if not pd.isna(comp_t) else 0
            })

    if found_params:
        print(f"  -> Wykryto dodatkowe parametry: {', '.join(sorted(found_params))}")

    df = pd.DataFrame(data_rows)
    if not df.empty:
        df['ORM'] = pd.Categorical(df['ORM'], categories=ORM_ORDER, ordered=True)
    return df


def draw_bar_chart(ax, df, x, y, title, ylabel, palette_name):
    sns.barplot(data=df, x=x, y=y, hue='ORM', palette=palette_name, ax=ax,
                errorbar=None, edgecolor='white', linewidth=2, width=0.6)

    max_val = df[y].max()
    ax.set_ylim(0, max_val * 1.25 if max_val > 0 else 1)

    for c in ax.containers:
        ax.bar_label(c, fmt='%.1f', padding=4, size=12, color='#555555')

    ax.set_title(title, fontsize=18, pad=15)
    ax.set_ylabel(ylabel)
    ax.set_xlabel("")


def create_heatmap(ax, df):
    hm_data = df.pivot_table(index='ORM', columns='Method', values='GC Time [ms]', aggfunc='sum', observed=True)
    existing_orms = [o for o in ORM_ORDER if o in hm_data.index]

    if not hm_data.empty and hm_data.sum().sum() > 0:
        hm_data = hm_data.loc[existing_orms]
        sns.heatmap(hm_data, annot=True, fmt=".1f", cmap="rocket_r", linewidths=1, ax=ax,
                    cbar_kws={'label': 'Czas GC (ms)'})
    else:
        ax.text(0.5, 0.5, 'Brak znaczących pauz GC', ha='center', va='center', fontsize=16)

    ax.set_title("Czas Pauz Garbage Collectora", fontsize=18, pad=15)
    ax.set_ylabel("")
    ax.set_xlabel("")


def create_pie_chart(ax, df):
    gc_counts = df.groupby('ORM', observed=True)['GC Count'].sum()
    gc_counts = gc_counts[gc_counts > 0]

    if not gc_counts.empty:
        color_map = dict(zip(ORM_ORDER, sns.color_palette("Set2", len(ORM_ORDER))))
        pie_colors = [color_map[orm] for orm in gc_counts.index]

        ax.pie(gc_counts, autopct='%1.1f%%', startangle=140,
               colors=pie_colors,
               wedgeprops=dict(width=0.4, edgecolor='white', linewidth=2),
               textprops={'fontsize': 14, 'weight': 'bold'}, pctdistance=1.2)
    else:
        ax.text(0.5, 0.5, 'Brak cykli GC', ha='center', va='center', fontsize=16)

    ax.axis('off')
    ax.set_title("Udział w ilości cykli GC", fontsize=18, pad=15)


def add_global_legend(fig, ax_source):
    handles, labels = ax_source.get_legend_handles_labels()
    fig.legend(handles, labels, loc='upper center', bbox_to_anchor=(0.5, 0.92), ncol=len(labels), frameon=False,
               fontsize=18, handlelength=3, handleheight=3)


def save_individual_plots(df, db_profile, out_dir):
    sns.set_theme(style="whitegrid", context="talk")

    fig_t, ax_t = plt.subplots(figsize=(12, 8))
    draw_bar_chart(ax_t, df, 'Method', 'Czas [ms/op]', f'Czas operacji - Baza: {db_profile.upper()}', 'ms / operację',
                   'Set2')
    ax_t.legend(title='ORM Provider', bbox_to_anchor=(1.02, 1), loc='upper left', handlelength=2, handleheight=2)
    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, f"{db_profile}_01_time.png"), dpi=300, facecolor='white', bbox_inches='tight')
    plt.close(fig_t)

    fig_m, ax_m = plt.subplots(figsize=(12, 8))
    draw_bar_chart(ax_m, df, 'Method', 'Alokacja [MB/op]', f'Alokacja pamięci RAM - Baza: {db_profile.upper()}',
                   'MB / operację', 'Set2')
    ax_m.legend(title='ORM Provider', bbox_to_anchor=(1.02, 1), loc='upper left', handlelength=2, handleheight=2)
    plt.tight_layout()
    plt.savefig(os.path.join(out_dir, f"{db_profile}_02_memory.png"), dpi=300, facecolor='white', bbox_inches='tight')
    plt.close(fig_m)


def generate_dashboard(df, db_profile, bench_num, output_path):
    sns.set_theme(style="whitegrid", context="talk")
    fig = plt.figure(figsize=(24, 16))

    fig.suptitle(f"PROFILER DASHBOARD | Scenariusz: {bench_num} | Baza: {db_profile.upper()}",
                 fontsize=28, fontweight='bold', color='black', y=0.98)

    show_jit = ADD_JIT and df['JIT Time [ms]'].sum() > 0

    gs_top = fig.add_gridspec(1, 2, top=0.82, bottom=0.55, wspace=0.2)
    gs_bot = fig.add_gridspec(1, 3 if show_jit else 2, top=0.45, bottom=0.08, wspace=0.3)

    ax_time = fig.add_subplot(gs_top[0, 0])
    draw_bar_chart(ax_time, df, 'Method', 'Czas [ms/op]', "Czas Operacji (Niższy = Lepszy)", "ms / operację", "Set2")

    ax_mem = fig.add_subplot(gs_top[0, 1])
    draw_bar_chart(ax_mem, df, 'Method', 'Alokacja [MB/op]', "Alokacja Pamięci RAM (Niższa = Lepsza)", "MB / operację",
                   "Set2")

    add_global_legend(fig, ax_time)

    ax_time.get_legend().remove()
    if ax_mem.get_legend():
        ax_mem.get_legend().remove()

    ax_gc_heat = fig.add_subplot(gs_bot[0, 0])
    create_heatmap(ax_gc_heat, df)

    ax_gc_pie = fig.add_subplot(gs_bot[0, 1])
    create_pie_chart(ax_gc_pie, df)

    if show_jit:
        ax_jit = fig.add_subplot(gs_bot[0, 2])
        draw_bar_chart(ax_jit, df, 'Method', 'JIT Time [ms]', "Narzut Kompilacji JIT", "ms", "Set2")
        if ax_jit.get_legend():
            ax_jit.get_legend().remove()

    plt.savefig(output_path, dpi=300, facecolor='white', bbox_inches='tight')
    plt.close(fig)


def main():
    os.makedirs(PLOT_DIR, exist_ok=True)

    files = glob.glob(os.path.join(JSON_DIR, '*.json'))
    if not files:
        print(f"Brak plików .json w folderze {JSON_DIR}.")
        return

    print(f"Znaleziono {len(files)} plików do przetworzenia.")

    files.sort(key=os.path.getmtime, reverse=True)
    if not PROCESS_ALL:
        files = files[:N_LATEST]
        print(f"Przetwarzam tylko {N_LATEST} najnowszych plików.")

    for file in files:
        print(f"\n--- Rozpoczęto przetwarzanie pliku: {os.path.basename(file)} ---")
        df = load_data(file)
        if df.empty:
            continue

        db_profile, bench_num = parse_filename(file)
        print(f"  -> Rozpoznano bazę danych: {db_profile.upper()}, Scenariusz nr: {bench_num}")
        out_dir = os.path.join(PLOT_DIR, bench_num)
        os.makedirs(out_dir, exist_ok=True)

        print("  -> Zapisywanie wykresów indywidualnych.")
        save_individual_plots(df, db_profile, out_dir)
        dashboard_path = os.path.join(out_dir, f"{db_profile}_03_dashboard.png")
        print(f"  -> Zapisywanie głównego dashboardu: {os.path.basename(dashboard_path)}.")
        generate_dashboard(df, db_profile, bench_num, dashboard_path)
        print(f"  -> Zakończono z sukcesem. Zapisano w folderze: {out_dir}")


if __name__ == '__main__':
    main()