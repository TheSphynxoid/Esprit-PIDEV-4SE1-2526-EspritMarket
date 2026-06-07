import {
  Component,
  ElementRef,
  AfterViewInit,
  OnDestroy,
  NgZone
} from '@angular/core';
import * as THREE from 'three';

interface FloatingObject {
  mesh: THREE.Object3D;
  basePos: THREE.Vector3;
  floatSpeed: THREE.Vector3;
  floatAmplitude: THREE.Vector3;
  rotSpeed: THREE.Vector3;
  phase: THREE.Vector3;
  breatheSpeed: number;
  breatheAmount: number;
}

@Component({
  selector: 'app-hero-canvas',
  standalone: true,
  template: '',
  styles: [
    `
      :host {
        position: absolute;
        inset: 0;
        display: block;
        pointer-events: none;
        z-index: 1;
      }
    `
  ]
})
export class HeroCanvasComponent implements AfterViewInit, OnDestroy {
  private renderer!: THREE.WebGLRenderer;
  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private animationId = 0;
  private floatingObjects: FloatingObject[] = [];
  private particleSystem!: THREE.Points;
  private particleSpeeds!: Float32Array;
  private mouse = new THREE.Vector2(0, 0);
  private targetMouse = new THREE.Vector2(0, 0);
  private clock = new THREE.Clock();
  private destroyed = false;

  private resizeHandler = () => this.onResize();
  private mouseHandler = (e: MouseEvent) => this.onMouseMove(e);
  private scrollHandler = () => this.onScroll();

  constructor(private el: ElementRef, private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    this.ngZone.runOutsideAngular(() => {
      try {
        this.initRenderer();
        this.initScene();
        this.initCamera();
        this.createFloatingShapes();
        this.createParticles();
        this.addLights();
        this.addListeners();
        this.animate();
      } catch {
        // WebGL not available — graceful fallback
      }
    });
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    cancelAnimationFrame(this.animationId);
    window.removeEventListener('resize', this.resizeHandler);
    window.removeEventListener('mousemove', this.mouseHandler);
    window.removeEventListener('scroll', this.scrollHandler);

    if (this.renderer) {
      this.scene.traverse((obj) => {
        if (obj instanceof THREE.Mesh || obj instanceof THREE.Points) {
          obj.geometry.dispose();
          const mat = obj.material;
          if (Array.isArray(mat)) {
            mat.forEach((m) => m.dispose());
          } else {
            (mat as THREE.Material).dispose();
          }
        }
      });
      this.renderer.dispose();
      if (this.renderer.domElement.parentNode) {
        this.renderer.domElement.parentNode.removeChild(this.renderer.domElement);
      }
    }
  }

  private initRenderer(): void {
    this.renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.setClearColor(0x000000, 0);
    this.renderer.domElement.style.display = 'block';
    this.el.nativeElement.appendChild(this.renderer.domElement);
  }

  private initScene(): void {
    this.scene = new THREE.Scene();
  }

  private initCamera(): void {
    const aspect = window.innerWidth / window.innerHeight;
    this.camera = new THREE.PerspectiveCamera(50, aspect, 0.1, 100);
    this.camera.position.set(0, 0, 7);
    this.camera.lookAt(0, 0, 0);
  }

  private createFloatingShapes(): void {
    // Large wireframe icosahedron — hero shape
    const icoGeo = new THREE.IcosahedronGeometry(2.0, 1);
    const icoEdges = new THREE.EdgesGeometry(icoGeo);
    const icoLine = new THREE.LineSegments(
      icoEdges,
      new THREE.LineBasicMaterial({
        color: 0xc8102e,
        transparent: true,
        opacity: 0.18
      })
    );
    this.addObject(icoLine, new THREE.Vector3(2.0, 0.2, -1.5),
      { speed: [0.15, 0.12, 0.08], amp: [0.4, 0.3, 0.15], rot: [0.001, 0.002, 0.0005], phase: [0, 0, 0], breathe: [0.5, 0.04] });

    // Medium wireframe torus knot
    const tkGeo = new THREE.TorusKnotGeometry(0.6, 0.2, 64, 8, 2, 3);
    const tkMat = new THREE.MeshStandardMaterial({
      color: 0xc8102e,
      metalness: 0.6,
      roughness: 0.3,
      transparent: true,
      opacity: 0.35
    });
    this.addObject(new THREE.Mesh(tkGeo, tkMat), new THREE.Vector3(-3.0, 1.8, -1),
      { speed: [0.25, 0.2, 0.15], amp: [0.3, 0.4, 0.2], rot: [0.004, 0.003, 0.002], phase: [1, 2, 0], breathe: [0.3, 0.03] });

    // Solid octahedron
    const octGeo = new THREE.OctahedronGeometry(0.45, 0);
    const octMat = new THREE.MeshStandardMaterial({
      color: 0xea9f97,
      metalness: 0.7,
      roughness: 0.2,
      transparent: true,
      opacity: 0.65
    });
    this.addObject(new THREE.Mesh(octGeo, octMat), new THREE.Vector3(-2.8, -1.2, 0.5),
      { speed: [0.4, 0.35, 0.25], amp: [0.25, 0.35, 0.2], rot: [0.007, 0.005, 0.003], phase: [2, 0, 1], breathe: [0.4, 0.05] });

    // Wireframe dodecahedron
    const dodGeo = new THREE.DodecahedronGeometry(0.5, 0);
    const dodEdges = new THREE.EdgesGeometry(dodGeo);
    const dodLine = new THREE.LineSegments(
      dodEdges,
      new THREE.LineBasicMaterial({
        color: 0xea9f97,
        transparent: true,
        opacity: 0.25
      })
    );
    this.addObject(dodLine, new THREE.Vector3(3.5, 2.2, -2),
      { speed: [0.3, 0.25, 0.18], amp: [0.2, 0.3, 0.15], rot: [0.005, 0.004, 0.003], phase: [3, 1, 2], breathe: [0.35, 0.03] });

    // Small glowing spheres (accent dots)
    const spherePositions = [
      { pos: new THREE.Vector3(-1.2, 2.8, -0.5), color: 0xc8102e },
      { pos: new THREE.Vector3(2.5, -2.2, 0.8), color: 0xea9f97 },
      { pos: new THREE.Vector3(4.0, 0.8, -2.5), color: 0xf5e1dd },
      { pos: new THREE.Vector3(-3.5, 0.3, -3), color: 0xc8102e },
      { pos: new THREE.Vector3(0.5, -2.8, -1), color: 0xea9f97 },
      { pos: new THREE.Vector3(-0.8, 3.2, -3), color: 0xffffff },
    ];
    spherePositions.forEach((s, i) => {
      const geo = new THREE.SphereGeometry(0.08, 16, 16);
      const mat = new THREE.MeshStandardMaterial({
        color: s.color,
        emissive: s.color,
        emissiveIntensity: 0.6,
        metalness: 0.3,
        roughness: 0.5,
        transparent: true,
        opacity: 0.8
      });
      this.addObject(new THREE.Mesh(geo, mat), s.pos, {
        speed: [0.5 + i * 0.08, 0.4 + i * 0.07, 0.3 + i * 0.06],
        amp: [0.15, 0.2, 0.1],
        rot: [0, 0, 0],
        phase: [i * 1.3, i * 0.9, i * 1.7],
        breathe: [0.6 + i * 0.1, 0.06]
      });
    });

    // Ring — large orbiting wireframe ring
    const ringGeo = new THREE.TorusGeometry(3.0, 0.01, 8, 80);
    const ringMat = new THREE.MeshBasicMaterial({
      color: 0xc8102e,
      transparent: true,
      opacity: 0.08
    });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = Math.PI / 3;
    this.addObject(ring, new THREE.Vector3(0, 0, -2),
      { speed: [0.05, 0.04, 0.03], amp: [0, 0, 0], rot: [0.001, 0.0005, 0], phase: [0, 0, 0], breathe: [0.2, 0.01] });
  }

  private addObject(
    mesh: THREE.Object3D,
    basePos: THREE.Vector3,
    opts: {
      speed: number[];
      amp: number[];
      rot: number[];
      phase: number[];
      breathe: number[];
    }
  ): void {
    mesh.position.copy(basePos);
    this.scene.add(mesh);
    this.floatingObjects.push({
      mesh,
      basePos: basePos.clone(),
      floatSpeed: new THREE.Vector3(...opts.speed),
      floatAmplitude: new THREE.Vector3(...opts.amp),
      rotSpeed: new THREE.Vector3(...opts.rot),
      phase: new THREE.Vector3(...opts.phase),
      breatheSpeed: opts.breathe[0],
      breatheAmount: opts.breathe[1]
    });
  }

  private createParticles(): void {
    const count = 250;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(count * 3);
    this.particleSpeeds = new Float32Array(count * 3);
    const colors = new Float32Array(count * 3);

    const palette = [
      new THREE.Color(0xc8102e),
      new THREE.Color(0xea9f97),
      new THREE.Color(0xffffff),
      new THREE.Color(0xf5e1dd)
    ];

    for (let i = 0; i < count; i++) {
      const i3 = i * 3;
      positions[i3] = (Math.random() - 0.5) * 20;
      positions[i3 + 1] = (Math.random() - 0.5) * 14;
      positions[i3 + 2] = (Math.random() - 0.5) * 12 - 2;

      this.particleSpeeds[i3] = (Math.random() - 0.5) * 0.004;
      this.particleSpeeds[i3 + 1] = (Math.random() - 0.5) * 0.004;
      this.particleSpeeds[i3 + 2] = (Math.random() - 0.5) * 0.002;

      const c = palette[Math.floor(Math.random() * palette.length)];
      colors[i3] = c.r;
      colors[i3 + 1] = c.g;
      colors[i3 + 2] = c.b;
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));

    const texture = this.createGlowTexture();

    const material = new THREE.PointsMaterial({
      size: 0.12,
      map: texture,
      transparent: true,
      opacity: 0.7,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
      vertexColors: true,
      sizeAttenuation: true
    });

    this.particleSystem = new THREE.Points(geometry, material);
    this.scene.add(this.particleSystem);
  }

  private createGlowTexture(): THREE.Texture {
    const canvas = document.createElement('canvas');
    canvas.width = 64;
    canvas.height = 64;
    const ctx = canvas.getContext('2d')!;
    const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32);
    gradient.addColorStop(0, 'rgba(255, 255, 255, 1)');
    gradient.addColorStop(0.15, 'rgba(255, 255, 255, 0.6)');
    gradient.addColorStop(0.4, 'rgba(200, 16, 46, 0.15)');
    gradient.addColorStop(1, 'rgba(200, 16, 46, 0)');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 64, 64);
    return new THREE.CanvasTexture(canvas);
  }

  private addLights(): void {
    this.scene.add(new THREE.AmbientLight(0xffffff, 0.5));

    const dir = new THREE.DirectionalLight(0xffffff, 0.7);
    dir.position.set(5, 5, 5);
    this.scene.add(dir);

    const point = new THREE.PointLight(0xc8102e, 1.5, 25);
    point.position.set(0, 0, 4);
    this.scene.add(point);

    const point2 = new THREE.PointLight(0xea9f97, 0.8, 20);
    point2.position.set(-4, 2, 2);
    this.scene.add(point2);
  }

  private addListeners(): void {
    window.addEventListener('resize', this.resizeHandler);
    window.addEventListener('mousemove', this.mouseHandler);
    window.addEventListener('scroll', this.scrollHandler, { passive: true });
  }

  private onResize(): void {
    const w = window.innerWidth;
    const h = window.innerHeight;
    this.camera.aspect = w / h;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(w, h);
  }

  private onMouseMove(e: MouseEvent): void {
    this.targetMouse.x = (e.clientX / window.innerWidth) * 2 - 1;
    this.targetMouse.y = -(e.clientY / window.innerHeight) * 2 + 1;
  }

  private onScroll(): void {
    const scrollY = window.scrollY;
    const heroH = window.innerHeight;
    const progress = Math.min(scrollY / (heroH * 0.6), 1);
    this.renderer.domElement.style.opacity = String(1 - progress);
  }

  private animate(): void {
    if (this.destroyed) return;
    this.animationId = requestAnimationFrame(() => this.animate());

    const t = this.clock.getElapsedTime();

    this.mouse.x += (this.targetMouse.x - this.mouse.x) * 0.04;
    this.mouse.y += (this.targetMouse.y - this.mouse.y) * 0.04;

    this.camera.position.x = this.mouse.x * 0.4;
    this.camera.position.y = this.mouse.y * 0.3;
    this.camera.lookAt(0, 0, 0);

    for (const obj of this.floatingObjects) {
      obj.mesh.position.x =
        obj.basePos.x +
        Math.sin(t * obj.floatSpeed.x + obj.phase.x) * obj.floatAmplitude.x;
      obj.mesh.position.y =
        obj.basePos.y +
        Math.cos(t * obj.floatSpeed.y + obj.phase.y) * obj.floatAmplitude.y;
      obj.mesh.position.z =
        obj.basePos.z +
        Math.sin(t * obj.floatSpeed.z + obj.phase.z) * obj.floatAmplitude.z;

      obj.mesh.rotation.x += obj.rotSpeed.x;
      obj.mesh.rotation.y += obj.rotSpeed.y;
      obj.mesh.rotation.z += obj.rotSpeed.z;

      const scale =
        1 + Math.sin(t * obj.breatheSpeed) * obj.breatheAmount;
      obj.mesh.scale.setScalar(scale);
    }

    const posAttr = this.particleSystem.geometry.attributes['position'] as THREE.BufferAttribute;
    const pos = posAttr.array as Float32Array;
    for (let i = 0; i < pos.length; i += 3) {
      pos[i] += this.particleSpeeds[i];
      pos[i + 1] += this.particleSpeeds[i + 1];
      pos[i + 2] += this.particleSpeeds[i + 2];

      if (Math.abs(pos[i]) > 10) this.particleSpeeds[i] *= -1;
      if (Math.abs(pos[i + 1]) > 7) this.particleSpeeds[i + 1] *= -1;
      if (Math.abs(pos[i + 2]) > 6) this.particleSpeeds[i + 2] *= -1;
    }
    posAttr.needsUpdate = true;

    this.particleSystem.rotation.y = t * 0.01;

    this.renderer.render(this.scene, this.camera);
  }
}
